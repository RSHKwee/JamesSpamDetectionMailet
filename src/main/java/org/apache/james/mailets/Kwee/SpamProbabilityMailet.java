package org.apache.james.mailets.Kwee;

public class SpamProbabilityMailet extends GenericMailet {
  @Override
  public void service(Mail mail) throws MessagingException {
      double probability = calculateSpamProbability(mail); // Jouw spam detectie logica
      mail.getMessage().setHeader("X-MessageIsSpamProbability", 
          String.format("%.2f", probability));
  }

  private double calculateSpamProbability(Mail mail) {
      // Voorbeeld: combineer meerdere checks
      double probability = 0.0;
      
      if (checkDNSBL(mail)) probability += 0.3;
      if (checkSPF(mail)) probability += 0.2;
      if (checkContent(mail)) probability += 0.5;
      
      return Math.min(1.0, probability); // Max 1.0
  }
}