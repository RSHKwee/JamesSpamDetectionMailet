package org.apache.james.mailets.Kwee;

import org.apache.mailet.*;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class AntiSpamMailet extends GenericMailet {
  private static final Logger LOGGER = LoggerFactory.getLogger(AntiSpamMailet.class);
  private static String AntiSpamInfo = "Custom AntiSpam Mailet v1.0";

  // Configuratie
  private List<String> dnsblServers;
  private double spamThreshold;
  private Map<String, Double> spamKeywords;
  private boolean greylistingEnabled;
  private final Cache<String, Boolean> greyListCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
      .build();

  // SPF authorized networks (vereenvoudigd)
  private static final Map<String, List<String>> SPF_AUTHORIZED_NETWORKS = Map.of("gmail.com",
      Arrays.asList("64.233.160.0/19", "66.102.0.0/20", "72.14.192.0/18"), "outlook.com",
      Arrays.asList("40.92.0.0/15", "40.107.0.0/16", "52.96.0.0/12"), "yahoo.com",
      Arrays.asList("66.196.64.0/18", "68.142.192.0/18", "72.30.0.0/16"), "domain.com", Arrays.asList("127.0.0.1"));
  /*
 * @formatter:off
 *  DNs Host             score
 v  zen.spamhaus.org       5
 v  bl.spamcop.net         3
   dnsbl.njabl.org        1
   psbl.surriel.com       1
   virbl.dnsbl.bit.nl     1
   b.barracudacentral.org 2

   SURBL servers
   multi.surbl.org        1

@formatter:on
 */

  @Override
  public void init() throws MessagingException {
    // DNSBL servers
    //@formatter:off
    dnsblServers = Arrays.asList(getInitParameter(
        "dnsbl.servers",
        "zen.spamhaus.org," + 
        "bl.spamcop.net," + 
        "dnsbl.sorbs.net," + 
        "spam.dnsbl.sorbs.net," + 
        "dnsbl.njabl.org," +
        "psbl.surriel.com," +
        "virbl.dnsbl.bit.nl," +
        "b.barracudacentral.org," +
        "bl.blocklist.de")
        .split(","));

    // Spam threshold
    spamThreshold = Double.parseDouble(getInitParameter("spam.threshold", "0.85"));

    // Greylisting
    greylistingEnabled = Boolean.parseBoolean(getInitParameter("greylisting.enabled", "true"));

    // Spam keywords met weights
    spamKeywords = new ConcurrentHashMap<>();
 
    String keywordsConfig = getInitParameter("spam.keywords",
        "viagra:0.8," +
        "casino:0.7," +
        "loan:0.6," +
        "free:0.5," +
        "money:0.6," + 
        "profit:0.5" +
        "Save Up To:0.5" );
    //@formatter:on
    Arrays.stream(keywordsConfig.split(",")).forEach(kv -> {
      String[] parts = kv.split(":");
      if (parts.length == 2) {
        spamKeywords.put(parts[0].toLowerCase(), Double.parseDouble(parts[1]));
      }
    });

    LOGGER.info("Initialized AntiSpamMailet with " + dnsblServers.size() + " DNSBL servers and " + spamKeywords.size()
        + " spam keywords");
  }

  @Override
  public void service(Mail mail) throws MessagingException {
    double probability = 0.0;
    try {
      probability = calculateSpamProbability(mail);
      setSpamHeaders(mail, probability);

      if (probability >= spamThreshold) {
        handleSpam(mail, probability);
      }
    } catch (Exception e) {
      LOGGER.info("AntiSpam processing error for mail " + mail.getName() + ": " + e.getMessage());
      setSpamHeaders(mail, probability);
    }
  }

  private double calculateSpamProbability(Mail mail) throws MessagingException, IOException {
    double probability = 0.0;

    // DNSBL check (30% weight)
    if (checkDNSBL(mail)) {
      probability += 0.3;
      setHeader(mail, "X-DNSBL-Hit", "true");
    } else {
      setHeader(mail, "X-DNSBL-Hit", "false");
    }

    // SPF check (20% weight)
    if (checkSPF(mail)) {
      probability += 0.2;
      setHeader(mail, "X-SPF-Fail", "true");
    } else {
      setHeader(mail, "X-SPF-Fail", "false");
    }

    // Greylisting (10% weight)
    if (greylistingEnabled && checkGreyList(mail)) {
      probability += 0.1;
      setHeader(mail, "X-Greylist", "NEW");
    } else {
      setHeader(mail, "X-Greylist", "-");
    }

    // Content analysis (40% weight)
    double contentScore = calculateContentScore(mail);
    if (contentScore > 0) {
      probability += Math.min(0.4, contentScore);
      setHeader(mail, "X-Spam-Content-Score", String.format("%.2f", contentScore));
    } else {
      setHeader(mail, "X-Spam-Content-Score", String.format("%.2f", 0.0));
    }

    return Math.min(1.0, probability);
  }

  private boolean checkDNSBL(Mail mail) {
    String ip = mail.getRemoteAddr();
    if (ip == null || ip.isBlank()) {
      return false;
    }

    return dnsblServers.parallelStream().filter(server -> server != null && !server.isBlank()).anyMatch(server -> {
      try {
        String query = reverseIP(ip) + "." + server;
        Lookup lookup = new Lookup(query, Type.A);
        SimpleResolver resolver = new SimpleResolver();
        resolver.setTimeout(Duration.ofSeconds(2));
        lookup.setResolver(resolver);

        Record[] records = lookup.run();
        boolean isListed = records != null && records.length > 0;

        if (isListed) {
          LOGGER.debug("DNSBL hit: " + server + " for IP: " + ip);
          setHeader(mail, "X-DNSBL-Server", server);
        }

        return isListed;
      } catch (Exception e) {
        LOGGER.info("DNSBL query failed for " + server + ": " + e.getMessage());
        return false;
      }
    });
  }

  private boolean checkSPF(Mail mail) {
    return mail.getMaybeSender().asOptional().map(sender -> {
      try {
        String clientIP = mail.getRemoteAddr();
        String senderDomain = sender.getDomain().asString();

        boolean isAuthorized = isIPAuthorized(clientIP, senderDomain);
        if (!isAuthorized) {
          LOGGER.info("SPF failure: IP " + clientIP + " not authorized for " + senderDomain);
        }

        return !isAuthorized;
      } catch (Exception e) {
        LOGGER.info("SPF check failed: " + e.getMessage());
        return false;
      }
    }).orElse(false);
  }

  private boolean checkGreyList(Mail mail) {
    String recipient = mail.getRecipients().iterator().next().asString();
    String sender = mail.getMaybeSender().asString();
    String triplet = mail.getRemoteAddr() + "|" + sender + "|" + recipient;

    // Check if triplet exists first
    Boolean cachedValue = greyListCache.getIfPresent(triplet);
    if (cachedValue != null) {
      return cachedValue;
    }

    // If not present, add it and return true (new triplet)
    greyListCache.put(triplet, false);
    LOGGER.debug("Greylist new triplet: " + triplet);
    return true;
  }

  private double calculateContentScore(Mail mail) throws MessagingException, IOException {
    String content = getMailContent(mail);
    if (content == null || content.isBlank()) {
      return 0.0;
    }

    content = content.toLowerCase();
    final String finalContent = content;

    return spamKeywords.entrySet().stream().filter(entry -> finalContent.contains(entry.getKey()))
        .mapToDouble(entry -> {
          LOGGER.debug("Spam keyword detected: " + entry.getKey());
          return entry.getValue();
        }).sum();
  }

  private String getMailContent(Mail mail) throws MessagingException, IOException {
    try {
      Object content = mail.getMessage().getContent();
      if (content instanceof String) {
        return (String) content;
      } else if (content instanceof Multipart) {
        Multipart multipart = (Multipart) content;
        for (int i = 0; i < multipart.getCount(); i++) {
          Part bodyPart = multipart.getBodyPart(i);
          if (bodyPart.isMimeType("text/plain") && bodyPart.getContent() instanceof String) {
            return (String) bodyPart.getContent();
          }
        }
      }
      return "";
    } catch (Exception e) {
      LOGGER.info("Content extraction failed: " + e.getMessage());
      return "";
    }
  }

  private boolean isIPAuthorized(String ip, String domain) {
    List<String> authorizedNetworks = SPF_AUTHORIZED_NETWORKS.getOrDefault(domain, Collections.emptyList());
    return authorizedNetworks.stream().anyMatch(cidr -> isIPInCIDR(ip, cidr));
  }

  private boolean isIPInCIDR(String ip, String cidr) {
    try {
      // Vereenvoudigde CIDR check - vervang door echte IP math library
      String[] cidrParts = cidr.split("/");
      String network = cidrParts[0];
      return ip.startsWith(network.substring(0, network.lastIndexOf('.')));
    } catch (Exception e) {
      return false;
    }
  }

  private String reverseIP(String ip) {
    if (ip.contains(":")) {
      // IPv6 (vereenvoudigd)
      return Arrays.stream(ip.split(":")).flatMap(s -> Arrays.stream(s.split(""))).filter(c -> !c.isEmpty()).reduce("",
          (a, b) -> b + "." + a);
    } else {
      // IPv4
      String[] octets = ip.split("\\.");
      return octets[3] + "." + octets[2] + "." + octets[1] + "." + octets[0];
    }
  }

  private void setSpamHeaders(Mail mail, double probability) throws MessagingException {
    setHeader(mail, "X-MessageIsSpamProbability", String.format("%.2f", probability));
    setHeader(mail, "X-Spam-Level", getSpamLevel(probability));
    setHeader(mail, "X-Spam-Flag", probability >= spamThreshold ? "YES" : "NO");

  }

  private void setHeader(Mail mail, String name, String value) throws MessagingException {
    mail.getMessage().setHeader(name, value);
  }

  private String getSpamLevel(double probability) {
    int level = (int) Math.ceil(probability * 10);
    return "▮".repeat(level) + "▯".repeat(10 - level);
  }

  private void handleSpam(Mail mail, double probability) throws MessagingException {
    setHeader(mail, "X-Spam-Status", "YES");

    String quarantineFolder = getInitParameter("quarantine.folder");
    if (quarantineFolder != null) {
      mail.setAttribute(Attribute.convertToAttribute("FOLDER", quarantineFolder));
    }

    LOGGER.info("Spam detected: " + mail.getName() + " - Probability: " + String.format("%.2f", probability)
        + " - Action: " + (quarantineFolder != null ? "Quarantine" : "Reject"));
  }

  @Override
  public String getMailetInfo() {
    return AntiSpamInfo;
  }
}