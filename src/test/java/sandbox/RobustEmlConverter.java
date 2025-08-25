package sandbox;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class RobustEmlConverter {

  public static MimeMessage convertEmlToMimeMessage(File emlFile) throws Exception {
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

  // Alternative method that tries to fix common EML issues
  public static MimeMessage convertEmlWithFix(File emlFile) throws Exception {
    Session session = Session.getInstance(new Properties());

    try {
      // First try the standard approach
      try (InputStream is = new FileInputStream(emlFile)) {
        return new MimeMessage(session, is);
      }

    } catch (MessagingException e) {
      System.out.println("Standard method failed, trying alternative approach...");

      // Read file as text and fix common issues
      String emlContent = readFileAsString(emlFile);
      emlContent = fixCommonEmlIssues(emlContent);

      // Convert back to input stream
      try (InputStream is = new ByteArrayInputStream(emlContent.getBytes(StandardCharsets.UTF_8))) {
        return new MimeMessage(session, is);
      }
    }
  }

  private static String readFileAsString(File file) throws IOException {
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\r\n");
      }
    }
    return content.toString();
  }

  private static String fixCommonEmlIssues(String emlContent) {
    // Fix missing headers
    if (!emlContent.contains("MIME-Version:")) {
      emlContent = "MIME-Version: 1.0\r\n" + emlContent;
    }

    // Ensure proper line endings
    emlContent = emlContent.replaceAll("(?<!\r)\n", "\r\n");

    return emlContent;
  }

  public static void debugMessage(MimeMessage message) throws MessagingException, IOException {
    if (message == null) {
      System.out.println("Message is null");
      return;
    }

    System.out.println("=== Debug Message Info ===");

    // Check and display all headers
    System.out.println("Headers:");
    java.util.Enumeration<Header> headers = message.getAllHeaders();
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      System.out.println(header.getName() + ": " + header.getValue());
    }

    // Basic message info
    System.out.println("\nBasic Info:");
    System.out.println("Subject: " + (message.getSubject() != null ? message.getSubject() : "NULL"));
    System.out.println("From: " + (message.getFrom() != null ? message.getFrom()[0] : "NULL"));

    // Check if message has content
    Object content = message.getContent();
    if (content != null) {
      System.out.println("Content type: " + content.getClass().getSimpleName());
      if (content instanceof String) {
        String textContent = (String) content;
        System.out.println("Content preview: " + textContent.substring(0, Math.min(100, textContent.length())));
      }
    } else {
      System.out.println("Content: NULL");
    }
  }

  public static void main(String[] args) {
    try {
      String emlFilePath = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\spam\\spam-1.eml";
      File emlFile = new File(emlFilePath);

      // Method 1: Standard approach
      System.out.println("Trying standard method...");
      MimeMessage message1 = convertEmlToMimeMessage(emlFile);
      debugMessage(message1);

      // Method 2: Alternative approach if first fails
      if (message1.getSubject() == null) {
        System.out.println("\nTrying alternative method...");
        MimeMessage message2 = convertEmlWithFix(emlFile);
        debugMessage(message2);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
