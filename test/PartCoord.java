import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class PartCoord implements Runnable {
    private int[] expectedParts;
    private Map<String, PrintWriter> writers;
    private ArrayList<Map<String,String>> rounds;
    private int currentRound;
    private String currentVote;
    private int pport;
    private String thisVote;
    private int failureCond;
    private List<BlockingQueue<VoteToken>> allQueues;
    private int timeout;
    private Participant participant;
    private boolean connectionsMade;
    private int noActiveParts;
    List<BlockingQueue<VoteToken>> toRemove;



    public PartCoord(int pport, List<BlockingQueue<VoteToken>> allQueues, Participant participant, int timeout, int failureCond, List<BlockingQueue<VoteToken>> toRemove) {
        this.allQueues = allQueues;
        this.pport = pport;
        this.participant = participant;
        this.timeout = timeout;
        this.failureCond = failureCond;
        connectionsMade = false;
        this.toRemove = toRemove;

    }


    @Override
    public void run() {
        boolean repeatNeeded;
        do {
            repeatNeeded = false;
            beginVote();
            while (!participant.isResest()) {
                try {
                    System.out.println("COORD: wait for reset signal");
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("CORD: got reset signal");
            repeatNeeded = true;
        } while (repeatNeeded);

    }

    private void beginVote() {
        participant.setResest(false);
        boolean finished = false;
        rounds = new ArrayList<>();
        setCurrentVoteInit(participant.getInitialVote());
        if (!connectionsMade) {
            expectedParts = (participant.getExpectedParts()); //TODO not reset safe
            noActiveParts = expectedParts.length;
            startStreams(); //not reset safe (reset data strucutes at the end?)
        }


        currentRound = 1;

        broadcastVote();


        while(noActiveParts != allQueues.size()){
            //spin
            System.out.println("CORD: Expected length not met");
        }

        while(!finished) {

            for (BlockingQueue<VoteToken> q : allQueues) { //TODO better than for
                System.out.println("CORD: polling");
                try {
                        VoteToken vt = q.poll(timeout, TimeUnit.MILLISECONDS);
                        if (vt != null)
                            addVote(vt.getVotes()[0][0], vt.getVotesAsString());
                        else
                            System.out.println("CORD: VT = null");

                } catch (InterruptedException e) {
                    //TODO A too remove;
                    System.out.println("CORD responding too slow");
                    e.printStackTrace();
                }
            }

            if (!toRemove.isEmpty()){
                System.out.println("DSFSADFAS");
                allQueues.remove(toRemove.get(0));
                toRemove.remove(0);
                noActiveParts--;
            }

            System.out.println("CORD: round finished");
            if (compareRounds()) {
                System.out.println("CORD: duplicate rounds, waking participant");
                if (failureCond == 2){
                    System.out.println("CORD: Ended for error code 2");
                    Runtime.getRuntime().halt(0);

                }
                participant.setFinishedVote(true, currentVote);
                synchronized (participant) {
                    participant.notify();
                }
                finished = true;
                //for reset----------------------
                rounds = new ArrayList<>();

            } else {

                currentVote = combineVotes();
                currentRound++;
                System.out.println("CORD: current vote = " + currentVote);
                System.out.println("CORD: current round = " + currentRound);
                if (rounds.size() <= currentRound) {
                    System.out.println("CORD: added new round = " + currentRound);
                    rounds.add(currentRound, new HashMap<>());
                }
                broadcastVote();
            }
        }
    }

    public void startStreams(){
        System.out.println("CORD: Populating writers");
        writers = new HashMap<>();
        try {

            for(int port: expectedParts){
                Socket tempSocket = new Socket("127.0.0.1", port);
                writers.put(String.valueOf(port), new PrintWriter(tempSocket.getOutputStream()));
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        connectionsMade = true;
    }

    public void setCurrentVoteInit(String currentVote){
        this.currentVote = currentVote;
        this.thisVote = currentVote.split(" ")[1];
        System.out.println("CORD: current vote set to " + currentVote);
        rounds.add(0,new HashMap<>());
        rounds.add(1,new HashMap<>());
        rounds.get(0).put(String.valueOf(pport), currentVote);
        currentRound = 1;
    }

    public synchronized void addVote(String id, String votes){
        System.out.println("CORD: adding vote ---- " + votes);
        rounds.get(currentRound).put(id, votes);
//        int i = currentRound;
//        boolean added = false;
//        while (!added) {
//            //reached a new round, make one
//            if (rounds.size() <= i) {
//                System.out.println("CORD: new round add in weird way");
//                rounds.add(i,new HashMap<>());
//            }
//
//            //if not in the current round, add
//            if (!rounds.get(i).containsKey(id)) {
//                System.out.println("--------" + votes);
//                rounds.get(i).put(id, votes);
//                //exit loop
//                System.out.println("CORD: added to current round");
//                added = true;
//            } else {
//                //else increase round
//                i++;
//                System.out.println("CORD: already in round");
//                added = false;
//            }
//        }
    }


    private String combineVotes(){
        Map<String, String> votes = new HashMap<>();
        //for the data in the current round
        for(String partVotes : rounds.get(currentRound).keySet()){
            System.out.println("partvote: "+partVotes);
            //make a token with each string stored
            VoteToken tempTok = new VoteToken(rounds.get(currentRound).get(partVotes));
            String[][] pairing = tempTok.getVotes();

            //add each id and vote option into the hashmap
            for (String[] row : pairing){
                votes.put(row[0],row[1]);
                System.out.println("adding " + Arrays.toString(row));
            }
        }

        System.out.println(currentVote);
        System.out.println(rounds.get(currentRound-1).get(String.valueOf(pport)));
        VoteToken tempTok = new VoteToken(currentVote);
        String[][] pairing = tempTok.getVotes();

        //add each id and vote option into the hashmap
        for (String[] row : pairing){
            votes.put(row[0],row[1]);
            System.out.println("adding " + Arrays.toString(row));
        }


        //remove own vote if there
        votes.remove(String.valueOf(pport));

        //turn hashmap to string
        //first add own vote to the front
        StringBuilder sb = new StringBuilder();
        sb.append(pport);
        sb.append(" ");
        sb.append(thisVote);
        sb.append(" ");

        for (String id : votes.keySet()){
            sb.append(id);
            sb.append(" ");
            sb.append(votes.get(id));
            sb.append(" ");
        }
        System.out.println("CORD: new vote is " + sb.toString());
        return sb.toString();
    }


    public boolean compareRounds(){
        System.out.println("CORD comparing round " + currentRound + " & " + (currentRound-1));
        if (currentRound == 0)
            return false;
        else if (rounds.get(currentRound).equals(rounds.get(currentRound-1))){
            return true;
        } else {
            return false;
        }
    }

    public void broadcastVote(){
        System.out.println("CORD: broadcasting votes...");
        //sends votes collected to all participants
        int count = 0;
        for (PrintWriter pw : writers.values()){
            if ((count >= 1) && (failureCond == 1)){
                //pw.close();
                System.out.println("CORD: Ended for error code 1");
                Runtime.getRuntime().halt(0);
                return;
            } else {
                pw.println(new VoteToken(currentVote).createMessage());
                pw.flush();
                count++;
            }
        }
    }


}