package sandbox;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailets.Kwee.AntiSpamMailet;
import org.apache.james.mailets.Kwee.library.MimeMsgsTarBz2Archive;

import org.apache.mailet.Mail;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiSpamMain {
  private static final Logger LOGGER = LoggerFactory.getLogger(AntiSpamMain.class);
  private static int teller = 0;

  public static void main(String[] args) throws IOException {
    AntiSpamMailet mailet = new AntiSpamMailet();
    MailetContext mailetContext;
    FakeMailetConfig mailetConfig;

    // try {
    mailetContext = mock(MailetContext.class);
    //@formatter:off
    mailetConfig = FakeMailetConfig.builder()
        .mailetName("AntiSpamMailet")
        .mailetContext(mailetContext)
        .setProperty("dnsbl.server", 
            "zen.spamhaus.org," +
            "bl.spamcop.net," +
            "dnsbl.sorbs.net," + 
            "spam.dnsbl.sorbs.net," + 
            "dnsbl.njabl.org," +
            "psbl.surriel.com," +
            "virbl.dnsbl.bit.nl," +
            "b.barracudacentral.org," +
            "bl.blocklist.de")       
        .setProperty("greylisting.enabled", "true")
        .build();
    try {
      //@formatter:on
      mailet.init(mailetConfig);
      //@formatter:off
      // ham
      // String mailBestand = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\ham\\20021010_easy_ham.tar.bz2";
      //spam
      String mailBestand = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\spam\\20030228_spam.tar.bz2";
      //  String mailBestand = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\spam\\20021010_spam.tar.bz2";

      //@formatter:on
      MimeMsgsTarBz2Archive archive;
      archive = new MimeMsgsTarBz2Archive(mailBestand);
      int aantalFiles = archive.getTotalMimeMsgs();
      System.out.println("Archief: " + mailBestand);
      System.out.println("Aantal mails: " + aantalFiles);
      List<MimeMessage> mimeMessages = archive.getMimeMsgs();
      teller = 0;
      mimeMessages.forEach(msg -> {
        FakeMail mail = createSpamTestMail(msg);
        try {
          mailet.service(mail);
          String[] probabilityHeader = mail.getMessage().getHeader("X-MessageIsSpamProbability");
          double probability = Double.parseDouble(probabilityHeader[0].replace(",", "."));
          LOGGER.info(teller + ": probability: " + probability);
          teller++;
        } catch (MessagingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });
    } catch (MessagingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static FakeMail createSpamTestMail(MimeMessage message) {
    try {
      // message = AdvancedEmlConverter.loadMimeMessageFromEml(emlFilePath);
      // MimeMessage message = emlConv.convertEmlToMimeMessage(emlFilePath);
      // AdvancedEmlConverter.displayMessageInfo(message);

      // Make Mail object
      Mail mail;
      mail = FakeMail.builder().name("spam-test-mail").mimeMessage(message).sender("sender@domain.com")
          .recipient("recipient@domain.com").build();
      return (FakeMail) mail;
    } catch (MessagingException e) {
      LOGGER.error("Error parsing EML file: " + e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error: " + e.getMessage());
    }
    return null;
  }
}