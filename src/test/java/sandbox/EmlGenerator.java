package sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmlGenerator {

    public static void main(String[] args) {
        // Definieer de onderdelen van de e-mail
        String from = "fraud@example.com";
        String to = "victim@example.com";
        String subject = "Win een miljoen euro!";
        String body = "Dit is de body van een phishing e-mail met een link naar http://malicious-site.com.";
        
        // Genereer de inhoud van het .eml bestand
        String emlContent = generateEmlContent(from, to, subject, body);
        
        // Sla de inhoud op als een .eml bestand
        String filename = "generated_spam_email.eml";
        try {
            Files.write(Paths.get(filename), emlContent.getBytes());
            System.out.println("Bestand succesvol aangemaakt: " + filename);
        } catch (IOException e) {
            System.err.println("Fout bij het aanmaken van het bestand: " + e.getMessage());
        }
    }

    private static String generateEmlContent(String from, String to, String subject, String body) {
        // Een StringBuilder is efficiënt voor het samenvoegen van strings
        StringBuilder eml = new StringBuilder();
        
        // Voeg de headers toe. Let op: volgorde is niet cruciaal, maar dit is standaard.
        eml.append("From: ").append(from).append("\n");
        eml.append("To: ").append(to).append("\n");
        eml.append("Subject: ").append(subject).append("\n");
        // Deze MIME-Version header is belangrijk voor een correcte weergave
        eml.append("MIME-Version: 1.0\n");
        // Dit geeft het content type aan. 'text/plain' betekent gewone tekst zonder opmaak.
        eml.append("Content-Type: text/plain; charset=\"UTF-8\"\n");
        // Voeg een custom header toe die vaak door spamfilters wordt gebruikt
        eml.append("X-Spam-Flag: YES\n");
        // Een lege regel scheidt headers van de body
        eml.append("\n");
        
        // Voeg de body van de e-mail toe
        eml.append(body);
        
        return eml.toString();
    }
}
