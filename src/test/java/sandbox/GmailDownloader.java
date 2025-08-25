package sandbox;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Base64;

public class GmailDownloader {
  private static final String APPLICATION_NAME = "Gmail Downloader";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
  private static final String CREDENTIALS_FILE_PATH = "/path/to/credentials.json";
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final String LABEL_NAME = "Your_Label_Name"; // e.g., "INBOX"
  private static final String OUTPUT_DIR = "./emails/";

  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(CREDENTIALS_FILE_PATH));
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
        clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline").build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  public static void main(String[] args) throws Exception {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME).build();

    // Create output directory
    Files.createDirectories(Paths.get(OUTPUT_DIR));

    // Fetch messages with the specified label
    String user = "me";
    ListMessagesResponse response = service.users().messages().list(user)
        .setLabelIds(Collections.singletonList(LABEL_NAME)).execute();
    List<Message> messages = response.getMessages();

    if (messages != null) {
      for (Message message : messages) {
        Message msg = service.users().messages().get(user, message.getId()).setFormat("raw").execute();

        // Decode raw data
        byte[] emailBytes = Base64.getUrlDecoder().decode(msg.getRaw());
        String fileName = OUTPUT_DIR + message.getId() + ".eml";

        // Write to .eml file
        Files.write(Paths.get(fileName), emailBytes);
        System.out.println("Downloaded: " + fileName);
      }
    }
  }
}
