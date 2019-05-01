import java.io.*;
import java.net.Socket;

public class Participant {

    public static void main(String[] args) throws Exception {
        int cport;
        int pport;
        timeout
        cporti hpporti htimeouti hfailurecondi
    }

    public void init() throws IOException {

        // Connect directly to the IRC server.

        Socket socket = new Socket(server, 6667);
        pleaselogin


        // Log on to the server.

        writer.write("NICK " + nick + "\r\n");
        writer.write("USER " + login + " 8 * : Java IRC Hacks Bot\r\n");
        writer.flush( );

        // Read lines from the server until it tells us we have connected.
        String line = null;
        while ((line = reader.readLine( )) != null) {
            if (line.indexOf("004") >= 0) {
                // We are now logged in.
                break;

            }
            else if (line.indexOf("433") >= 0) {
                System.out.println("Nickname is already in use.");
                return;
            }
        }


        // Join the channel.

        writer.write("JOIN " + channel + "\r\n");
        writer.flush( );

        // Keep reading lines from the server.

        while ((line = reader.readLine( )) != null) {
            if (line.toLowerCase( ).startsWith("PING ")) {
                // We must respond to PINGs to avoid being disconnected.
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.write("PRIVMSG " + channel + " :I got pinged!\r\n");
                writer.flush( );
            }

            else {

                // Print the raw line received by the bot.

                System.out.println(line);

            }

        }

    }

}
