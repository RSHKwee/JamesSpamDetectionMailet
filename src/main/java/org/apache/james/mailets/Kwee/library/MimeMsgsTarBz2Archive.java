package org.apache.james.mailets.Kwee.library;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

// Main container for the entire archive
public class MimeMsgsTarBz2Archive {
  private String archiveName;
  private List<MimeMessage> mimeMessages;

  public MimeMsgsTarBz2Archive(String archiveName) throws IOException, MessagingException {
    this.archiveName = archiveName;
    this.mimeMessages = new ArrayList<>();
    load(archiveName);
  }

  // Getters and setters
  public void addMimeMsg(MimeMessage mail) {
    this.mimeMessages.add(mail);
  }

  public List<MimeMessage> getMimeMsgs() {
    return this.mimeMessages;
  }

  public int getTotalMimeMsgs() {
    return mimeMessages.size();
  }

  public String getArchiveName() {
    return archiveName;
  }

  private void load(String filePath) throws IOException, MessagingException {
    this.archiveName = filePath;
    try (FileInputStream fis = new FileInputStream(filePath);
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(fis);
        TarArchiveInputStream tarIn = new TarArchiveInputStream(bzIn)) {

      TarArchiveEntry entry;
      while ((entry = tarIn.getNextTarEntry()) != null) {
        if (!entry.isDirectory()) {
          MimeMessage msg = AdvancedEmlConverter.loadMimeMessageFromEml(tarIn);
          mimeMessages.add(msg);
        }
      }
    }
  }
}