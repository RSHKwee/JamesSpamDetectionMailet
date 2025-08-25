package org.apache.james.mailets.Kwee;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import javax.mail.internet.MimeMessage;

public class AntiSpamMailetTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AntiSpamMailetTest.class);

  private AntiSpamMailet mailet;
  private MailetContext mailetContext;
  private FakeMailetConfig mailetConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    mailet = new AntiSpamMailet();
    mailetContext = mock(MailetContext.class);
    //@formatter:off
    mailetConfig = FakeMailetConfig.builder()
        .mailetName("AntiSpamMailet")
        .mailetContext(mailetContext)
        .setProperty("dnsbl.server", "zen.spamhaus.org,bl.spamcop.net")
        .setProperty("greylisting.enabled", "false")
       
        .build();
    //@formatter:on
  }

  @Test
  public void testSpamDetection() throws Exception {
    mailet.init(mailetConfig);

    FakeMail mail = createSpamTestMail();
    mailet.service(mail);

    String[] probabilityHeader = mail.getMessage().getHeader("X-MessageIsSpamProbability");
    double probability = Double.parseDouble(probabilityHeader[0].replace(",", "."));
    assertTrue(probability >= 0.6);
  }

  @Test
  public void testHamDetection() throws Exception {
    mailet.init(mailetConfig);

    FakeMail mail = createTestMail();
    mailet.service(mail);

    String[] probabilityHeader = mail.getMessage().getHeader("X-MessageIsSpamProbability");

    double probability = Double.parseDouble(probabilityHeader[0].replace(",", "."));
    assertTrue(probability <= 0.21);
  }

  // Local routines
  /**
   * Create a Mail
   * 
   * @return Composed mail
   */
  private FakeMail createSpamTestMail() {
    MimeMessage message;
    try {
      //@formatter:off
      message = MimeMessageBuilder
          .mimeMessageBuilder()
          .setSubject("Spam test mail")
          .setText("Buy cheap viagra and win casino money!")
          .build();
      return FakeMail
          .builder()
          .name("mail1")
          .mimeMessage(message)
          .sender("sender@domain.com")
          .recipient("recipient@domain.com")
          .build();
      //@formatter:on
    } catch (Exception e) {
      LOGGER.info(e.getMessage().toString());
    }
    return null;
  }

  /**
   * Create a Mail
   * 
   * @return Composed mail
   */
  private FakeMail createTestMail() {
    MimeMessage message;
    try {
      //@formatter:off
      message = MimeMessageBuilder
          .mimeMessageBuilder()
          .setSubject("Test mail")
          .setText("Hello world!")
          .build();
      return FakeMail
          .builder()
          .name("mail1")
          .mimeMessage(message)
          .sender("sender@domain.com")
          .recipient("recipient@domain.com")
          .build();
      //@formatter:on
    } catch (Exception e) {
      LOGGER.info(e.getMessage().toString());
    }
    return null;
  }

}