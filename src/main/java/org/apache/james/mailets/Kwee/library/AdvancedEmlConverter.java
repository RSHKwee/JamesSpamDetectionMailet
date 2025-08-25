package org.apache.james.mailets.Kwee.library;

import javax.mail.*;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class AdvancedEmlConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(MimeMessage.class);

  public static MimeMessage loadMimeMessageFromEml(String emlFilePath) throws MessagingException, IOException {
    Session session = Session.getInstance(new Properties());

    try (InputStream inputStream = new FileInputStream(emlFilePath)) {
      MimeMessage msg = new MimeMessage(session, inputStream);
      return msg;
    }
  }

  public static MimeMessage loadMimeMessageFromEml(InputStream inputStream) throws MessagingException {
    Session session = Session.getInstance(new Properties());
    MimeMessage msg = new MimeMessage(session, inputStream);
    return msg;
  }

  public static MimeMessage convertEmlToMimeMessage(String emlFile) throws Exception {
    // Create session with proper properties
    Properties props = new Properties();
    props.put("mail.mime.address.strict", "false");
    props.put("mail.mime.charset", "UTF-8");

    Session session = Session.getInstance(props);

    try (InputStream inputStream = new FileInputStream(emlFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      // Read the entire file first to ensure completeness
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }

      // Create input stream from the byte array
      byte[] emlData = baos.toByteArray();
      ByteArrayInputStream bais = new ByteArrayInputStream(emlData);
      MimeMessage message = new MimeMessage(session, bais);

      // Force parsing of headers
      message.saveChanges();
      return message;

    } catch (MessagingException e) {
      throw new Exception("Failed to parse EML file: " + e.getMessage(), e);
    }
  }

  public static void displayMessageInfo(MimeMessage message) throws MessagingException {
    LOGGER.info("=== Email Information ===");
    LOGGER.info("Subject: " + (message.getSubject() != null ? message.getSubject() : "No Subject"));
    LOGGER.info("From: " + arrayToString(message.getFrom()));
    LOGGER.info("To: " + arrayToString(message.getRecipients(Message.RecipientType.TO)));
    LOGGER.info("CC: " + arrayToString(message.getRecipients(Message.RecipientType.CC)));
    LOGGER.info("BCC: " + arrayToString(message.getRecipients(Message.RecipientType.BCC)));
    LOGGER.info("Sent Date: " + message.getSentDate());
    LOGGER.info("Received Date: " + message.getReceivedDate());

    // Get content type
    LOGGER.info("Content Type: " + message.getContentType());

    // Get message content
    try {
      Object content = message.getContent();
      if (content instanceof String) {
        LOGGER.info("Content: " + content.toString().substring(0, Math.min(200, content.toString().length())) + "...");
      } else if (content instanceof Multipart) {
        LOGGER.info("Multipart message with " + ((Multipart) content).getCount() + " parts");
      }
    } catch (IOException e) {
      LOGGER.info("Error reading content: " + e.getMessage());
    }
  }

  private static String arrayToString(Address[] addresses) {
    if (addresses == null || addresses.length == 0) {
      return "None";
    }
    StringBuilder sb = new StringBuilder();
    for (Address address : addresses) {
      if (sb.length() > 0)
        sb.append(", ");
      sb.append(address.toString());
    }
    return sb.toString();
  }
}