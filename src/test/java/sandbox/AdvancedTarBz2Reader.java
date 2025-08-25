package sandbox;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailets.Kwee.library.AdvancedEmlConverter;

public class AdvancedTarBz2Reader {

  public static void processTarBz2(String filePath, FileProcessor processor) throws IOException, MessagingException {
    try (FileInputStream fis = new FileInputStream(filePath);
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(fis);
        TarArchiveInputStream tarIn = new TarArchiveInputStream(bzIn)) {

      TarArchiveEntry entry;
      while ((entry = tarIn.getNextTarEntry()) != null) {
        if (!entry.isDirectory()) {
          // processor.processFile(entry.getName(), tarIn, entry.getSize());
          MimeMessage msg = AdvancedEmlConverter.loadMimeMessageFromEml(tarIn);
          AdvancedEmlConverter.displayMessageInfo(msg);
        }
      }
    }
  }

  public interface FileProcessor {
    void processFile(String fileName, InputStream inputStream, long fileSize) throws IOException;
  }

  // Example processor that prints file contents
  public static class ContentPrinter implements FileProcessor {
    @Override
    public void processFile(String fileName, InputStream inputStream, long fileSize) throws IOException {
      System.out.println("File: " + fileName);
      System.out.println("Size: " + fileSize + " bytes");

      byte[] content = new byte[(int) fileSize];
      int bytesRead = inputStream.read(content);

      if (bytesRead == fileSize) {
        System.out.println("Content:\n" + new String(content, StandardCharsets.UTF_8));
      } else {
        System.out.println("Warning: Could not read complete file");
      }
      System.out.println("----------------------------");
    }
  }

  public static void main(String[] args) {
    try {
      processTarBz2(
          "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\ham\\20021010_easy_ham.tar.bz2",
          new ContentPrinter());
    } catch (IOException | MessagingException e) {
      System.err.println("Error reading tar.bz2 file: " + e.getMessage());
    }
  }
}