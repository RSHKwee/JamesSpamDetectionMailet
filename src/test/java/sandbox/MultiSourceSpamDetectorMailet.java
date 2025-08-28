package sandbox;

import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiSourceSpamDetectorMailet extends GenericMailet {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiSourceSpamDetectorMailet.class);

  // SpamAssassin GTUBE teststring
  private static final String GTUBE = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";

  // Threat Intelligence API endpoint
  private String threatIntelApiUrl;

  // Bayes-filter parameters
  private Map<String, Double> spamProbabilities;
  private double defaultProbability = 0.4;
  private double spamThreshold = 0.95;

  @Override
  public void init() throws MessagingException {
    super.init();

    // Configureer Threat Intelligence API
    threatIntelApiUrl = getInitParameter("threatIntelUrl");

    // Laad Bayes-model (in productie zou dit uit een database/file komen)
    spamProbabilities = new HashMap<>();
    spamProbabilities.put("gratis", 0.98);
    spamProbabilities.put("winnaar", 0.97);
    spamProbabilities.put("actie", 0.85);
    spamProbabilities.put("beperkte tijd", 0.92);
  }

  @Override
  public void service(Mail mail) throws MessagingException {
    try {
      String senderIp = mail.getRemoteAddr();
      String content = extractContent(mail.getMessage());

      // Controleer op SpamAssassin GTUBE-pattern
      if (content.contains(GTUBE)) {
        markAsSpam(mail, "GTUBE spam pattern detected");
        // return;
      }

      // Controleer Threat Intelligence API
      if (checkThreatIntel(senderIp)) {
        markAsSpam(mail, "Blacklisted by threat intelligence: " + senderIp);
        // return;
      }

      // Bayes-spamfilter toepassen
      double spamProbability = calculateBayesProbability(content);
      if (spamProbability > spamThreshold) {
        markAsSpam(mail, String.format("Bayes spam probability %.2f%%", spamProbability * 100));
      }

    } catch (Exception e) {
      LOGGER.info("Error processing mail: " + mail.getName() + " - " + e.getMessage());
      // In productie: foutenlogboek bijhouden
    }
  }

  private String extractContent(MimeMessage message) throws MessagingException, IOException {
    // Vereenvoudigde content extractie (in productie: MIME-parser gebruiken)
    return message.getContent().toString();
  }

  private boolean checkThreatIntel(String ip) throws IOException {
    if (threatIntelApiUrl == null)
      return false;

    URL url = new URL(threatIntelApiUrl + "?ip=" + ip);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    if (conn.getResponseCode() == 200) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
        String response = reader.readLine();
        return Boolean.parseBoolean(response);
      }
    }
    return false;
  }

  private double calculateBayesProbability(String content) {
    // Tokenize content (vereenvoudigd)
    String[] tokens = content.toLowerCase().split("\\s+");
    double productSpam = 1.0;
    double productHam = 1.0;

    for (String token : tokens) {
      Double prob = spamProbabilities.get(token);
      if (prob == null)
        prob = defaultProbability;

      productSpam *= prob;
      productHam *= (1 - prob);
    }

    return productSpam / (productSpam + productHam);
  }

  private void markAsSpam(Mail mail, String reason) throws MessagingException {
    LOGGER.info("Marking mail as spam: " + mail.getName() + " - Reason: " + reason);
    mail.setAttribute("SPAM_REASON", reason);

    // Stuur door naar quarantaine of spamfolder
    getMailetContext().sendMail(mail, "spam-quarantine@yourdomain.com");

    // Of gebruik: mail.setState(Mail.GHOST); om te verwijderen
  }

  @Override
  public String getMailetInfo() {
    return "Multi-Source Spam Detection Mailet";
  }
}