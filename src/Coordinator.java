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

public class Coordinator
{
    private int port;
    private int parts;
    private String[] votingOptions;
    private int[] participants;
    private int timeout = 10*1000;

    private Map<String, PrintWriter> partMap;
    // Maps name to socket. Key is clientName, value is clientOut. */
    private Map<String,String> outcomesRecived;

    /**
     * For each client we create a thread that handles
     * all i/o with that client.
     */
    private class ServerThread extends Thread {
        private Socket partSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String id;

        ServerThread(Socket client) throws IOException {
            partSocket = client;
            System.out.println("new thread created for mystery participant");

            // Open I/O steams
            reader = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
            writer = new PrintWriter( new OutputStreamWriter( client.getOutputStream() ) );

            // Welcome message.
            writer.println( "You have been connected\n ");
            writer.flush();
        }

        public void run(){
            try {
                partSocket.setSoTimeout(timeout);

                Token token = null;
                Tokenizer tokenizer = new Tokenizer();

                // First, the part must send a join
                token = tokenizer.getToken(reader.readLine());
                if (!(token instanceof JoinToken)) {
                    System.out.println("First token not a join token");
                    partSocket.close();
                    return;
                }

                // Adds the ID to the list of parts
                //?? check ports ID - should be the port it's listing on
                id = ((JoinToken) token).getPportAsString();
                System.out.println(id + " has been identified");
                partMap.put(((JoinToken) token).getPportAsString(), writer);

                // If this succeeds, check if all parties have joined
                checkParticipants();

                //wait till all send initail data
                wait();

                //start sending info
                yell(new DetailsToken(participants).createMessage());
                yell(new VoteOptionsToken(votingOptions).createMessage());

                // process requests until client exits.
                token = tokenizer.getToken( reader.readLine() );
                if (!(token instanceof OutcomeToken)) {
                    System.out.println("Second token not a outcome token");
                    partSocket.close();
                    return;
                }

                outcomesRecived.put(((OutcomeToken) token).getOutcome(), ((OutcomeToken) token).getPartsAsString());

                partSocket.close();
                removeParticipant(id);
                checkOutcome();
            }
            catch (IOException | InterruptedException e) {
                System.err.println("Caught I/O Exception.");
                e.printStackTrace();
                removeParticipant(id);
            }
        }

        private void removeParticipant(String id){
            partMap.remove(id);
            System.out.println(id + " removed");
        }

        private void checkParticipants(){
            //if all parties have joined, send details and vote options
            if (partMap.size() == parts){
                notifyAll();
            }
        }

    }

    private synchronized void checkOutcome(){
        //if all parties have joined, send details and vote options
        if (outcomesRecived.size() == parts){
            System.out.println("all outcomes recived");
        }
    }

    // Send a message to all registered clients.
    private synchronized void yell(String msg)
    {
        Iterator iter = partMap.values().iterator();
        while (iter.hasNext()) {
            PrintWriter pw = (PrintWriter)iter.next();
            pw.println(msg);
            pw.flush();
        }
    }

    // Wait for a connection request.
    private void startListening() throws IOException{
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

    private void init(String[] args) throws IOException {
        //extraction of options
        port = Integer.parseInt(args[0]);
        parts = Integer.parseInt(args[1]);
        votingOptions = Arrays.copyOfRange(args, 2, args.length);
        participants = new int[parts];

        //confirm for user
        System.out.println("Coordinator started - Port: " + port);
        System.out.println("Expecting participant count: " + parts);
        System.out.println("Voting options: " + Arrays.toString(votingOptions));
        System.out.println("Expecting participant: " + Arrays.toString(participants));

        partMap = Collections.synchronizedMap(new HashMap<String, PrintWriter>(parts));
        outcomesRecived = Collections.synchronizedMap(new HashMap<String, String>(parts));

        startListening();
    }


    public static void main(String[] args) throws IOException {

        System.out.println("Server started on <port>");
        Coordinator coord =  new Coordinator();
        coord.init(args);

    }
}




