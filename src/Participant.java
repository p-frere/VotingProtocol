import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Participant {

    Socket socket = null;
    int cport;
    int pport;
    int timeout;
    int failurecond;

    public static void main(String[] args) throws Exception {

    }

    public void initSocket() throws IOException {
        try {
            socket = new Socket("127.0.0.1", 1342);
        } catch (IOException e) {
            System.out.println("socket delceration error");
            e.printStackTrace();
        }

        // accepting something from the server
        Scanner serverScanner = new Scanner(socket.getInputStream());
        String temp;

        // prints input stream to chat
        while (true) {
            temp = serverScanner.nextLine();
            print("\n" + temp);
        }
    }

    // sends submitted text to server
    public void Send(String text) {

        // passes to the server with the username
        PrintStream ps;
        try {
            ps = new PrintStream(socket.getOutputStream());
            ps.println( text);
            ps.flush();

        } catch (IOException e) {
            System.out.println("error in print stream");
            e.printStackTrace();
        }
    }
     while (!(token instanceof ExitToken)) {
        if (token instanceof YellToken)
            yell(_clientName, ((YellToken)token)._msg);
        if (token instanceof TellToken)
            tell(_clientName, ((TellToken)token)._rcpt, ((TellToken)token)._msg);

        if (token instanceof OutcomeToken)
            outcomesRecived.put(_clientName, (TellToken)token);


        // Ignore JoinToken
        token = tokenizer.getToken(reader.readLine());
    }



    public void init(int cport, int pport) throws IOException {

        // Connect directly to the IRC server.

        Socket socket = new Socket(server, 6667);

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
