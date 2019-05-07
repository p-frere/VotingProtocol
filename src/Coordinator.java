import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.*;

public class Coordinator
{
    private int port;
    private int parts;
    private String[] votingOptions;
    private int timeout = 10*1000;

    private Map<String, PrintWriter> partMap;
    // Maps name to socket. Key is clientName, value is clientOut. */
    private Map<String,String> outcomesRecived;

    //A Thread manages I/O with each participant
    private class ServerThread extends Thread {
        private Socket partSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String id;

        ServerThread(Socket client) throws IOException {
            partSocket = client;
        }

        public void run(){
            try {
                // Open I/O steams
                reader = new BufferedReader( new InputStreamReader( partSocket.getInputStream() ) );
                writer = new PrintWriter( new OutputStreamWriter( partSocket.getOutputStream() ) );


                //partSocket.setSoTimeout(timeout);

                // First, the participant must send a join token
                Token token = null;
                Tokenizer tokenizer = new Tokenizer();

                String temp = reader.readLine();
                token = tokenizer.getToken(temp);
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

                //wait for all participants to join
                waitForJoins();


                //start sending info
                tell(id, new DetailsToken(getParts(id)).createMessage());
                tell(id, new VoteOptionsToken(votingOptions).createMessage());


                // wait until votes received from participants exits.
                token = tokenizer.getToken( reader.readLine() );
                if (!(token instanceof OutcomeToken)) {
                    System.out.println("Second token not a outcome token");
                    partSocket.close();
                    return;
                }

                outcomesRecived.put(id, ((OutcomeToken) token).getOutcome() + " - " + ((OutcomeToken) token).getPartsAsString());

                //closes channels of communication
                partSocket.close();
                removeParticipant(id);
                checkOutcome();
            }
            catch (IOException e) {
                System.err.println("Caught I/O Exception.");
                e.printStackTrace();
                removeParticipant(id);
            }
        }

        //remove a participant for the list
        private void removeParticipant(String id){
            partMap.remove(id);
            System.out.println(id + " removed");
        }

        //get a String[] of all the participants excluding current self
        private String[] getParts(String id){
            Iterator<String> it = partMap.keySet().iterator();
            String current;
            String[] partsExludingMe = new String[parts-1];
            int count = 0;
            while (it.hasNext()){
                if (!(current = it.next()).equals(id)){
                    partsExludingMe[count] = current;
                    count++;
                }
            }
            return partsExludingMe; //new String[]{"1","2"};
        }


    }

    //if all parties have joined, send details and vote options
    private synchronized void checkOutcome(){
        if (outcomesRecived.size() == parts){
            System.out.println("all outcomes recived");
            printOutcome();
        }
    }

    //wait for all parties to join
    private synchronized void waitForJoins(){
        if (partMap.size() != parts) {

            while (partMap.size() != parts) {
                try {
                    System.out.println("waiting...");
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        } else {
            notifyAll();
        }

        System.out.println("stopped waiting");

    }

    //print outcome
    private synchronized void printOutcome(){
        for (String id: outcomesRecived.keySet()) {
            System.out.println();
            System.out.println("Result for " + id + ": " + outcomesRecived.get(id));
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


    //Send a message to the specified recipient.
    synchronized void tell(String id, String msg)
    {
        PrintWriter pw = partMap.get(id);
        if (pw == null)
            return; // No client with the specified name
        pw.println(msg);
        pw.flush();
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

        //confirm for user
        System.out.println("Coordinator started - Port: " + port);
        System.out.println("Expecting participant count: " + parts);
        System.out.println("Voting options: " + Arrays.toString(votingOptions));

        partMap = Collections.synchronizedMap(new HashMap<String, PrintWriter>(parts));
        outcomesRecived = Collections.synchronizedMap(new HashMap<String, String>(parts));

        startListening();
    }


    public static void main(String[] args) throws IOException {
        Coordinator coord =  new Coordinator();
        //coord.init(args);
        coord.init(new String[]{"1444", "3", "A", "B"});

    }
}




