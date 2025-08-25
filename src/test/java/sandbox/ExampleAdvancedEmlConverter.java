package sandbox;

import org.apache.james.mailets.Kwee.library.AdvancedEmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;

public class ExampleAdvancedEmlConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleAdvancedEmlConverter.class);

  public static void main(String[] args) {
    String emlFilePath = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\spam\\spam-1.eml";
    try {
      MimeMessage message = AdvancedEmlConverter.loadMimeMessageFromEml(emlFilePath);
      // MimeMessage message = emlConv.convertEmlToMimeMessage(emlFilePath);
      AdvancedEmlConverter.displayMessageInfo(message);

    } catch (FileNotFoundException e) {
      LOGGER.error("EML file not found: " + emlFilePath);
    } catch (MessagingException e) {
      LOGGER.error("Error parsing EML file: " + e.getMessage());
    } catch (IOException e) {
      LOGGER.error("IO Error: " + e.getMessage());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}