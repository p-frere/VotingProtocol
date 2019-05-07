import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class PartCoord extends Thread {
    Participant participant;
    private int[] expectedParts;
    private Map<String, PrintWriter> writers;
    private ArrayList<Map<String,String>> rounds;
    private int currentRound;
    private String currentVote;
    private int pport;
    private String thisVote;
    private int errorCode = 1;



    public PartCoord(Participant participant, int[] expectedParts, int pport) {
        this.participant = participant;
        this.expectedParts = expectedParts;
        this.pport = pport;
        rounds = new ArrayList<>();
    }

    @Override
    public void run() {
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
    }

    public void setCurrentVoteInit(String currentVote){
        this.currentVote = currentVote;
        this.thisVote = currentVote.split(" ")[1];
        System.out.println("CORD: current vote set to " + currentVote);
        rounds.add(0,new HashMap<>());
        rounds.get(0).put(String.valueOf(pport), currentVote);
        currentRound = 1;
    }

    public synchronized void addVote(String id, String votes){
   //     System.out.println("CORD: adding vote");
  //      System.out.println("CORD: current round = " + currentRound);
        int i = currentRound;
        boolean added = false;
        while (!added) {
            //reached a new round, make one
            if (rounds.size() <= i) {
  //              System.out.println("CORD: new round add");
                rounds.add(i,new HashMap<>());
            }

            //if not in the current round, add
            if (!rounds.get(i).containsKey(id)) {
                System.out.println("--------" + votes);
                rounds.get(i).put(id, votes);
                //exit loop
       //         System.out.println("CORD: added to current round");
                added = true;
            } else {
                //else increase round
                i++;
       //         System.out.println("CORD: already in round");
                added = false;
            }
        }


        if (isRoundFinished()) {
            System.out.println("CORD: round finished");
          if (compareRounds()) {
              System.out.println("CORD: duplicate rounds, waking participant");
              participant.votingFinished(currentVote);
          } else {
              //shop class running
              currentVote = combineVotes();
              currentRound++;
              if (rounds.size() < currentRound){
                  rounds.add(currentRound, new HashMap<>());
              }
              broadcastVote();
          }
        } else {
            System.out.println("CORD: round not finished");
        }
    }

    public synchronized void removeParticipant(){

    }

    public boolean isRoundFinished(){
        //if size is correct
       // System.out.println("------current round " + currentRound);
       // System.out.println("------- how many rounds " + rounds.size());
      //  System.out.println("------- current round size " +  rounds.get(currentRound).size());
       // System.out.println("------- expected length " +  expectedParts.length);

        return rounds.get(currentRound).size() == expectedParts.length;
    }

    private String combineVotes(){
        Map<String, String> votes = new HashMap<>();
        //for the data in the current round
        for(String partVotes : rounds.get(currentRound).keySet()){
            //make a token with each string stored
            VoteToken tempTok = new VoteToken(rounds.get(currentRound).get(partVotes));
            String[][] pairing = tempTok.getVotes();

            //add each id and vote option into the hashmap
            for (String[] row : pairing){
                votes.put(row[0],row[1]);
            }
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
//            if ((count >= 1) && (errorCode == 1)){
//                pw.close();
//                return;
//            } else {
                pw.println(new VoteToken(currentVote).createMessage());
                pw.flush();
                count++;
//            }
        }
    }


}