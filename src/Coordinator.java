import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * The Coordinator is able to handle up to _MAXCLIENTS clients
 * simultaneously.
 *
 * Supported vocabulary:
 * JOIN name - Request to join the group
 * YELL msg - Sends the msg to clients
 * TELL name msg - Sends the msg only the the specified client
 * EXIT - The client gets released by the server
 * @author Tim Norman, University of Southampton
 * @version 2.0
 */

public class Coordinator
{
    /** Max # of clients. */
    final int _MAXCLIENTS = 50;

    private Map<Integer,Token> partMap = Collections.synchronizedMap(new HashMap<Integer,Token>(_MAXCLIENTS));


    /** Maps name to socket. Key is clientName, value is clientOut. */
    private Map<Integer,PrintWriter> outcomesRecived = Collections.synchronizedMap(new HashMap<Integer,PrintWriter>(_MAXCLIENTS));

    /**
     * For each client we create a thread that handles
     * all i/o with that client.
     */
    private class ServerThread extends Thread {
        private Socket partSocket;
        private String _clientName;
        private BufferedReader br_clientIn;
        private PrintWriter pw_clientOut;

        ServerThread(Socket client) throws IOException {
            partSocket = client;

            // Open I/O steams
            br_clientIn = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
            pw_clientOut = new PrintWriter( new OutputStreamWriter( client.getOutputStream() ) );

            // Welcome message.
            pw_clientOut.println( "Welcome to Coordinator\n ");
            pw_clientOut.flush();
        }

        public void run(){
            try {
                Token token = null;
                Tokenizer tokenizer = new Tokenizer();

                // First, the part must send a join
                token = tokenizer.getToken(br_clientIn.readLine());
                if (!(token instanceof JoinToken)) {
                    partSocket.close();
                    return;
                }

                // Adds the ID to the list of parts
                // ??? Check the part's ID (Should be the port it's listingn on), Check it's not repeated ???
                if (!(register(_clientName = ((JoinToken)token)._name, pw_clientOut))) {
                    partSocket.close();
                    return;
                }

                // If this succeeds, check if all parties have joined
                checkParticipants();

                //wait till all send initail data
                wait();


                // process requests until client exits.
                token = tokenizer.getToken( br_clientIn.readLine() );
                while (!(token instanceof ExitToken)) {
                    if (token instanceof YellToken)
                        yell(_clientName, ((YellToken)token)._msg);
                    if (token instanceof TellToken)
                        tell(_clientName, ((TellToken)token)._rcpt, ((TellToken)token)._msg);

                    if (token instanceof OutcomeToken)
                        outcomesRecived.put(_clientName, (TellToken)token);


                    // Ignore JoinToken
                    token = tokenizer.getToken(br_clientIn.readLine());
                }


                partSocket.close();
                unregister(_clientName);
            }
            catch (IOException | InterruptedException e) {
                System.err.println("Caught I/O Exception.");
                unregister(_clientName);
            }
        }

        public void checkParticipants(){
            //if all parties have joined, send details and vote options
            if (partMap.size() == parts){
                yell(_clientName, ((YellToken)token)._msg);
            }

            //all will start
            notifyAll();
        }
    }


    /**
     * Attempts to register the client under the specified name.
     * Return true if successful.
     */
    boolean register(Integer port, PrintWriter out)
    {
        // if (_numOfClients >= _MAXCLIENTS)
        //     return false;

        // if (partMap.containsKey(name)) {
        //     System.err.println("Coordinator: Name already joined.");
        //     return false;
        // }

        try {
            partMap.put(port, out);
        }
        catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    /**
     * Unregisters the client with the specified name.
     */
    void unregister(String name)
    {
        partMap.remove(name);
        _numOfClients--;
        yell("Coordinator", name+" has exited.");
    }

    /**
     * Send a message to all registered clients.
     */
    synchronized void yell(String sender, String msg)
    {
        String txt = sender + ": " + msg;
        Iterator iter = partMap.values().iterator();
        while (iter.hasNext()) {
            PrintWriter pw = (PrintWriter)iter.next();
            pw.println(txt);
            pw.flush();
        }
    }

    /**
     * Send a message to the specified recipient.
     */
    synchronized void tell(String sender, String rcpt, String msg)
    {
        String txt = sender + ": " + msg;
        PrintWriter pw = partMap.get(rcpt);
        if (pw == null)
            return; // No client with the specified name
        pw.println(txt);
        pw.flush();
    }

    // Wait for a connection request.
    void startListening(int port, int parts) throws IOException{
        int joinedCnt = 0;
        ServerSocket listener = new ServerSocket(port);
        System.out.println("Listning...");

        while (joinedCnt <= parts) {
            Socket client = listener.accept();
            new ServerThread(client).start();
            joinedCnt++;
            System.out.println("Participant joined " + joinedCnt + "/" + parts);
        }

        listener.close();
        System.out.println("Stopped listning.");
    }

    void init(String[] args) throws IOException {
        //extraction of options
        port = Integer.parseInt(args[0]);
        parts = Integer.parseInt(args[1]);
        votingOptions = Arrays.copyOfRange(args, 2, args.length);
        participants = new int[parts];

        //confirm for user
        System.out.println("Coordinator started - Port: " + port);
        System.out.println("Expecting participant count: " + parts);

        //start listing
        startListening(port, parts);
    }

    private int port;
    private int parts;
    private String[] votingOptions;
    private int[] participants;
    public static void main(String[] args) throws IOException {

        System.out.println("Server started on <port>");
        Coordinator coord =  new Coordinator();
        coord.init(args);

    }
}




