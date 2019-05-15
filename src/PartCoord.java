import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class PartCoord implements Runnable {
    private int[] initPartSockets;
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

    private CountDownLatch resetCountdown;
    private CountDownLatch votesFinishedCountdown;
    private CountDownLatch roundCountdown;
    private List<Integer> deadList;

    public PartCoord(int pport, List<BlockingQueue<VoteToken>> allQueues, Participant participant, int timeout, int failureCond, List<BlockingQueue<VoteToken>> toRemove) {
        this.allQueues = allQueues;
        this.pport = pport;
        this.participant = participant;
        this.timeout = timeout;
        this.failureCond = failureCond;
        connectionsMade = false;
        this.toRemove = toRemove;

        resetCountdown = participant.getResetCountdown();
        deadList = participant.getDeadList();

    }


    @Override
    public void run() {

        beginVote();

        do {
            try {

                System.out.println("COORD: wait for reset signal");
                System.out.println("COORD: " + resetCountdown.getCount());
                if (!resetCountdown.await(10L, TimeUnit.SECONDS)) {
                    System.out.println("COORD: Timed out");
                    Runtime.getRuntime().halt(0);
                }

                System.out.println("CORD: got reset signal");
                resetCountdown = new CountDownLatch(1);
                beginVote();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (true);

    }

    private void beginVote() {
        //roundCountdown = participant.getRoundCountdown();
        votesFinishedCountdown = participant.getVotesFinishedCountdown();

        //setup for variables and votes
        boolean finished = false;
        rounds = new ArrayList<>();
        setCurrentVoteInit(participant.getInitialVote());

        //sets up connections, only need to do once
        if (!connectionsMade) {
            initPartSockets = (participant.getExpectedParts());
            noActiveParts = participant.getActiveParts();
            startStreams();
        }

        //sends vote data to other participants
        broadcastVote();

        //spins until correct number of participants joined
        while(noActiveParts != allQueues.size()){
            //spin
            System.out.println("CORD: Expected length not met");
        }

        while(!finished) {
            //try {
                System.out.println("CORD: All parts present, waiting on round...");
                //roundCountdown.await();

                for (int q = 0; q < allQueues.size(); q++){
                //for (BlockingQueue<VoteToken> q : allQueues) { //TODO better than for
//                        if(!q.isEmpty()) {
//                            VoteToken vt = q.poll();
//                            addVote(vt.getVotes()[0][0], vt.getVotesAsString());
//                        } else {
//                            System.out.println("CORD: null skipped");
//                        }

                    if(!deadList.contains(q)) { //if a is not marked as dead
                        VoteToken vt = null;
                        try {
                            vt = allQueues.get(q).poll(3L, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (vt != null)
                                addVote(vt.getVotes()[0][0], vt.getVotesAsString());
                    } else {
                        System.out.println("CORD: null skipped");
                    }
                }


//            } catch (InterruptedException e) {
//                System.out.println("COORD: interupted");
//                e.printStackTrace();
//            }

            System.out.println("CORD: round finished");
            if (compareRounds()) {
                System.out.println("CORD: duplicate rounds, waking participant");
                if (failureCond == 2){
                    System.out.println("CORD: Ended for error code 2");
                    Runtime.getRuntime().halt(0);

                }
                participant.setFinishedVote(true, currentVote);
                votesFinishedCountdown.countDown();
                System.out.println("COORD: votes finished countdown = " + votesFinishedCountdown.getCount());
                finished = true;

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

    //connects writers to all other participants
    public void startStreams(){
        System.out.println("CORD: Populating writers");
        writers = new HashMap<>();
        try {
            for(int port: initPartSockets){
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
        rounds.get(0).put(String.valueOf(pport), currentVote);
        rounds.add(1,new HashMap<>());
        currentRound = 1;
    }

    public synchronized void addVote(String id, String votes){
        System.out.println("CORD: adding vote ---- " + votes);
        rounds.get(currentRound).put(id, votes);
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
            //System.out.println("adding " + Arrays.toString(row));
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

    public String convertWithIteration(Map<String, ?> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        return mapAsString.toString();
    }

    public boolean compareRounds(){
        System.out.println("CORD comparing round " + currentRound + " & " + (currentRound-1));
        System.out.println("CORD: " + convertWithIteration(rounds.get(currentRound)));
        System.out.println("CORD: " + convertWithIteration(rounds.get(currentRound-1)));

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