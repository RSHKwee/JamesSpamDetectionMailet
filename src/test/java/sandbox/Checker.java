package sandbox;

import java.io.IOException;
import java.net.InetAddress;

public class Checker {

  public static void main(String[] args) throws IOException {
    String ip = "";
    boolean isBlacklisted = InetAddress.getByName(ip + ".zen.spamhaus.org").isReachable(1000);

    // zen.spamhaus.org (Spamhaus)
    // bl.spamcop.net (SpamCop)
    // dnsbl.sorbs.net (SORBS)

  }
}
