package sandbox;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailets.Kwee.library.MimeMsgsTarBz2Archive;

public class TestTarBz2Lezer {

  public static void main(String[] args) {
    String mailBestand = "D:\\Dev\\Github\\James Maillets\\SpamDetection\\src\\test\\resources\\mail\\ham\\20021010_easy_ham.tar.bz2";
    MimeMsgsTarBz2Archive archive;
    try {
      archive = new MimeMsgsTarBz2Archive(mailBestand);
      int aantalFiles = archive.getTotalMimeMsgs();
      System.out.println("Aantal mails: " + aantalFiles);
      List<MimeMessage> mimeMessages = archive.getMimeMsgs();
      System.out.println();
    } catch (IOException | MessagingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
