package org.apache.james.mailets.Kwee;

import org.apache.mailet.*;
import org.apache.mailet.base.GenericMailet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import javax.mail.MessagingException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AntiSpamMailet extends GenericMailet {
  private static final Logger LOGGER = LoggerFactory.getLogger(AntiSpamMailet.class);
  private List<String> dnsblServers;

  @Override
  public void init() throws MessagingException {
    // Standard DNSBL servers (same as hMailServer)
    //@formatter:off
    dnsblServers = Arrays.asList(getInitParameter("dnsbl.servers",
        "zen.spamhaus.org," + 
        "bl.spamcop.net," + 
        "dnsbl.sorbs.net," + 
        "spam.dnsbl.sorbs.net," + 
        "bl.blocklist.de," + 
        "dnsbl-1.uceprotect.net," + 
        "dnsbl-2.uceprotect.net," + 
        "dnsbl-3.uceprotect.net," + 
        "psbl.surriel.com," + 
        "b.barracudacentral.org")
        .split(","));
    //@formatter:on

    // Optioneel: logging van geladen servers
    LOGGER.info("Loaded DNSBL servers: " + dnsblServers);
  }

  @Override
  public void service(Mail mail) throws MessagingException {
    try {
      if (checkDNSBL(mail)) {
        mail.setAttribute(Attribute.convertToAttribute("X-SPAM", "true"));
      }
    } catch (Exception e) {
      LOGGER.info("AntiSpam error: " + e.getMessage());
    }
  }

  @Override
  public String getMailetInfo() {
    return "DNSBL AntiSpam Mailet";
  }

  // v ===== Private functions ====================
  //@formatter:off
  private Map<String, Integer> dnsblWeights = Map.of(
      "zen.spamhaus.org", 10, 
      "bl.spamcop.net", 8,
      "b.barracudacentral.org", 5
      // Other servers defaults to weight = 1
  );
  //@formatter:on

  private boolean checkDNSBL(Mail mail) {
    String ip = mail.getRemoteAddr();
    if (ip == null || ip.isBlank())
      return false;

    AtomicInteger spamScore = new AtomicInteger(0);
    int threshold = 5; // Minimum required score

    dnsblServers.parallelStream().forEach(server -> {
      try {
        String query = reverseIP(ip) + "." + server;
        Lookup lookup = new Lookup(query, Type.A);
        lookup.setResolver(new ExtendedResolver());

        if (lookup.run() != null) {
          int weight = dnsblWeights.getOrDefault(server, 1);
          spamScore.addAndGet(weight);
          LOGGER.info("DNSBL hit: " + server + " (+" + weight + ")");
        }
      } catch (Exception e) {
        LOGGER.info("DNSBL query failed: " + server, e);
      }
    });

    return spamScore.get() >= threshold;
  }

  private String reverseIP(String ip) {
    String[] octets = ip.split("\\.");
    return octets[3] + "." + octets[2] + "." + octets[1] + "." + octets[0];
  }
}