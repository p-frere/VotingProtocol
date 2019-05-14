import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Coordinator
{
    private int port;
    private int parts;
    private String[] votingOptions;
    private int timeout = 10*1000;
    boolean finished;
    private boolean restart;
    private final Object restartKey = new Object();


    private Map<String, PrintWriter> partMap;
    // Maps name to socket. Key is clientName, value is clientOut. */
    private BlockingQueue<OutcomeToken> outcomesRecived;

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
    }


    public synchronized boolean isRestart() {
        return restart;
    }

    public synchronized void setRestart(boolean restart) {
        this.restart = restart;
    }

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
                    System.out.println("ST: First token not a join token");
                    partSocket.close();
                    return;
                }

                // Adds the ID to the list of parts
                //?? check ports ID - should be the port it's listing on
                id = ((JoinToken) token).getPportAsString();
                System.out.println("ST: "+id + " has been identified");
                partMap.put(((JoinToken) token).getPportAsString(), writer);

                //wait for all participants to join
                waitForJoins();


                //start sending info
                tell(id, new DetailsToken(getParts(id)).createMessage());
                tell(id, new VoteOptionsToken(votingOptions).createMessage());


                // wait until votes received from participants exits.
                String newOutcome = reader.readLine();
                if (newOutcome == null){
                    throw new IOException();
                }
                token = tokenizer.getToken( newOutcome );
                if (!(token instanceof OutcomeToken)) {
                    System.out.println("ST: Second token not a outcome token");
                    partSocket.close();
                    return;
                }

                outcomesRecived.add((OutcomeToken)token);
                checkOutcome();


            //---------------------
                boolean repeatNeeded = false;
                do {
                    repeatNeeded = false;

                    try {
                        synchronized (restartKey) {
                            System.out.println("ST: waiting for condition check");
                            restartKey.wait();
                            System.out.println("ST: left waiting");
                        }
                    } catch (InterruptedException e) {
                        System.out.println("ST: timeout expired 2");
                        //closes channels of communication
                        partSocket.close();
                        removeParticipant(id);
                        //e.printStackTrace();
                    }

                    System.out.println("is restart = " + isRestart());

                    if (isRestart()) {
                        repeatNeeded = true;

                        // we might be improperly awoken here so we loop around to see if the
                        // condition is still true or if we timed out

                        outcomesRecived.clear();
                        tell(id, "RESTART");
                        System.out.println("ST: waiting again now for new outcome after restart...");

                        // wait until votes received from participants exits.
                        token = tokenizer.getToken(reader.readLine());
                        if (!(token instanceof OutcomeToken)) {
                            System.out.println("ST: Second token not a outcome token");
                            partSocket.close();
                            return;
                        }
                        System.out.println("ST: new outcome recived");

                        outcomesRecived.add((OutcomeToken) token);
                        checkOutcome();

                    }
                } while (repeatNeeded);




            }
            catch (IOException e) {
                System.err.println("ST: Caught I/O Exception.");
                //e.printStackTrace();
                removeParticipant(id);
            }
        }

        //remove a participant for the list
        private void removeParticipant(String id){
            partMap.remove(id);
            System.out.println(id + " removed");
            //This is needed if waiting end error 2 occures
            checkOutcome();
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
        //(id, ((OutcomeToken) token).getOutcome() + " - " + ((OutcomeToken) token).getPartsAsString());
        System.out.println("Check outcome" + outcomesRecived.size() + " " + partMap.size());
        if (outcomesRecived.size() == partMap.size()){
            System.out.println("all outcomes recived");
            setFinished(true);
            synchronized (this) {
                this.notify();
            }
        }
    }

    private void results(){
        while (!isFinished()) {
            try {
                System.out.println("results waiting...");
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        //each time queue is added to it is checked
        //wait until queue full or time out
        if (outcomesRecived.size() != parts){
            //restart votes
        }

        OutcomeToken out = outcomesRecived.poll();
        String result = out.getOutcome();
        String includedParts = out.getPartsAsString();
        while (!outcomesRecived.isEmpty()) {
            out = outcomesRecived.poll();
            if (!out.getOutcome().equals(result)){
                //some error
                System.out.println("ERROR results do not match");
            }
        }

        System.out.println("----"+result+" " +includedParts+"----");

        if(result.equals("null")) {
            System.out.println("Restart needed");
            setFinished(false);
            setRestart(true);
            synchronized (restartKey) {
                System.out.println("notifying key");
                restartKey.notifyAll();
            }
            results();

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
    private synchronized void tell(String id, String msg)
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

        do  {
            Socket client = listener.accept();
            client.setSoLinger(true, 0);
            new ServerThread(client).start();
            joinedCnt++;
            System.out.println("Participant joined " + joinedCnt + "/" + parts);
        } while (joinedCnt < parts);

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
        outcomesRecived = new LinkedBlockingQueue<>();

        startListening();
        results();
    }


    public static void main(String[] args) throws IOException {
        Coordinator coord =  new Coordinator();
        coord.init(args);
        //coord.init(new String[]{"1444", "2", "A", "B"});

    }
}




