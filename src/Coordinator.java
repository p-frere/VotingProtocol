import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Coordinator
{
    private int port;
    private int parts;
    private String[] votingOptions;
    private boolean finished;
    private boolean restart;
    private final Object restartKey = new Object();


    private Map<String, PrintWriter> partWriters;
    private BlockingQueue<OutcomeToken> outcomesRecived;

    private CountDownLatch outcomeCountdown;
    private CountDownLatch resetCountdown;

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


    public synchronized int getParts() {
        return parts;
    }

    public synchronized void setParts(int parts) {
        this.parts = parts;
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

                // First, the coord must read a join token
                Token token = null;
                Tokenizer tokenizer = new Tokenizer();
                String temp = reader.readLine();
                token = tokenizer.getToken(temp);
                if (!(token instanceof JoinToken)) {
                    System.out.println("ST: First token not a join token");
                    throw new IOException();
                }

                // Adds the ID to the list of parts
                id = ((JoinToken) token).getPportAsString();
                System.out.println("ST: "+id + " has been identified");
                partWriters.put(((JoinToken) token).getPportAsString(), writer);

                //wait for all participants to join
                waitForJoins();

                //start sending info
                tell(id, new DetailsToken(getPartsExcludingMe(id)).createMessage());
                tell(id, new VoteOptionsToken(votingOptions).createMessage());

                // wait until votes received from participants
                String newOutcome = reader.readLine();
                if (newOutcome == null){
                    throw new IOException();
                }
                token = tokenizer.getToken( newOutcome );
                if (!(token instanceof OutcomeToken)) {
                    System.out.println("ST: Second token not a outcome token");
                    throw new IOException();
                }

                //check if all outcomes are recived
                outcomesRecived.add((OutcomeToken)token);
                //checkOutcome();
                outcomeCountdown.countDown();
                resetCountdown.await();

            //-----------Reset----------

                do {
                    //wait to see if a reset is required
                    resetCountdown = new CountDownLatch(1);
                    //if it is send a restart token with updated voting options
                    tell(id, new RestartToken(votingOptions).createMessage());
                    //System.out.println("ST: waiting again now for new outcome after restart...");

                    // wait until votes received from participants exits.
                    temp = reader.readLine();
                    if (temp == null){
                        throw new IOException();
                    }
                    token = tokenizer.getToken(temp);
                    if (!(token instanceof OutcomeToken)) {
                        System.out.println("ST: token not a outcome token");
                        throw new IOException();
                    }

                    outcomesRecived.add((OutcomeToken) token);
                    outcomeCountdown.countDown();
                    resetCountdown.await();
                } while (true);

            }
            catch (IOException | InterruptedException e) {
                System.err.println("ST: Caught I/O Exception. Participant connection removed");
                //e.printStackTrace();
                removeParticipant(id);
            }
        }

        //remove a participant for the list
        private void removeParticipant(String id){
            partWriters.remove(id);
            int temp = getParts();
            setParts(temp-1);
            outcomeCountdown.countDown();
            //System.out.println(id + " removed");

        }

        //get a String[] of all the participants excluding current self
        private String[] getPartsExcludingMe(String id){
            Iterator<String> it = partWriters.keySet().iterator();
            String current;
            String[] partsExludingMe = new String[parts-1];
            int count = 0;
            while (it.hasNext()){
                if (!(current = it.next()).equals(id)){
                    partsExludingMe[count] = current;
                    count++;
                }
            }
            return partsExludingMe;
        }
    }

    //wait for all parties to join
    private synchronized void waitForJoins(){
        if (partWriters.size() != getParts()) {

            while (partWriters.size() != getParts()) {
                try {
                    //System.out.println("waiting...");
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        } else {
            notifyAll();
        }
        //System.out.println("stopped waiting");
    }

    private Coordinator(String[] args) throws IOException, InterruptedException {
        init(args);
        startListening();

        //System.out.println("Waiting on outcome latch");
        outcomeCountdown.await();
        System.out.println("COORD: Got all outcomes");

        results();
        close();
    }

    private void close(){
        Runtime.getRuntime().halt(0);
    }

    //calculates the results
    private void results() throws InterruptedException {
        if (outcomesRecived.isEmpty()){
            System.out.println("COORD: No outcomes recived");
            System.out.println("-Finished-");
            close();
        }

        OutcomeToken out = outcomesRecived.poll();
        String result = out.getOutcome();
        String includedParts = out.getPartsAsString();
        while (!outcomesRecived.isEmpty()) {
            out = outcomesRecived.poll();
            if (!out.getOutcome().equals(result)){
                //check to see if all outcomes match
                System.out.println("COORD: ERROR results do not match");
            }
        }

        System.out.println("----"+result+" " +includedParts+"----");

        if(result.equals("null")) {
            //restart is needed
            System.out.println("COORD: Restart needed");
            //reduce voting options
            votingOptions = Arrays.copyOfRange(votingOptions, 0, votingOptions.length-1);
            //reset latches and trigger reset latch
            outcomeCountdown = new CountDownLatch(getParts());
            outcomesRecived.clear();
            resetCountdown.countDown();

            //System.out.println("Waiting on outcome latch");
            outcomeCountdown.await();
            System.out.println("COORD: Recived all outcomes");

            results();

        } else {
            System.out.println("-Finished-");
        }
    }

    //Send a message to the specified recipient.
    private synchronized void tell(String id, String msg)
    {
        PrintWriter pw = partWriters.get(id);
        if (pw == null)
            return; // No client with the specified name
        pw.println(msg);
        pw.flush();
    }

    // Wait for a connection request.
    private void startListening() throws IOException{
        int joinedCnt = 0;
        ServerSocket listener = new ServerSocket(port);
        System.out.println("COORD: Listning...");

        do  {
            Socket client = listener.accept();
            client.setSoLinger(true, 0);
            new ServerThread(client).start();
            joinedCnt++;
            System.out.println("COORD: Participant joined " + joinedCnt + "/" + parts);
        } while (joinedCnt < parts);

        listener.close();
        //System.out.println("Stopped listning.");
    }

    private void init(String[] args) {
        //extraction of options
        port = Integer.parseInt(args[0]);
        parts = Integer.parseInt(args[1]);
        votingOptions = Arrays.copyOfRange(args, 2, args.length);

        //confirm for user
        System.out.println("Coordinator started - Port: " + port);
        System.out.println("Expecting participant count: " + parts);
        System.out.println("Voting options: " + Arrays.toString(votingOptions));

        //create new data structures
        partWriters = Collections.synchronizedMap(new HashMap<String, PrintWriter>(parts));
        outcomesRecived = new LinkedBlockingQueue<>();
        outcomeCountdown = new CountDownLatch(parts);
        resetCountdown = new CountDownLatch(1);
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        new Coordinator(args);
        //coord.init(new String[]{"1444", "2", "A", "B"});

    }
}




