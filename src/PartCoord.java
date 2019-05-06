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



    public PartCoord(Participant participant, int[] expectedParts) {
        this.participant = participant;
        this.expectedParts = expectedParts;
        rounds = new ArrayList<>();
    }

    @Override
    public void run() {

    }

    public void startStreams(){

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

    public void setCurrentVote(String currentVote){
        this.currentVote = currentVote;
    }

    public synchronized void addVote(String id, String votes){
        int i = currentRound;
        while (i < rounds.size()) {
            //reached a new round, make one
            if (rounds.get(i).isEmpty()) {
                rounds.add(i,new HashMap<>());
            }

            //if not in the current round, add
            if (!rounds.get(i).containsKey(id)) {
                rounds.get(i).put(id, votes);
                //exit loop
                i = rounds.size();
            } else {
                //else increase round
                i++;
            }
        }


        if (isRoundFinished()) {
          compareRounds();
          currentVote = combineVotes();
          currentRound++;
        }
    }

    public synchronized void removeParticipant(){

    }

    public boolean isRoundFinished(){
        //if size is correct
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

        //turn hashmap to string
        StringBuilder sb = new StringBuilder();
        for (String id : votes.keySet()){
            sb.append(id);
            sb.append(" ");
            sb.append(votes.get(id));
            sb.append(" ");
        }

        return sb.toString();
    }


    public void compareRounds(){
        if (currentRound == 0)
            return;
        else if (rounds.get(currentRound).equals(rounds.get(currentRound-1))){
            //if current and previous round the same
            //awaken thread
            participant.notify();
            //participant must retrieve current round data
        } else {
            return;
        }
        //if current and previous round the same
        //awaken thread
        participant.notify();
        //participant must retrieve current round data
    }

    public void broadcastVote(){
        //sends votes collected to all participants
        for (PrintWriter pw : writers.values()){
            pw.println(new VoteToken(currentVote).createMessage());
            pw.flush();
        }
    }

}