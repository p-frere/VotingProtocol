import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {

    Socket socket = null;
    int cport = 1444;
    int pport;
    int timeout;
    int failurecond;
    int[] parts;
    String[] voteOptions;
    String vote;
    PrintWriter writer;
    BufferedReader reader;

    private Map<String, String> votesRecived;

    public static void main(String[] args) throws Exception {
        System.out.println("PART: Started");

        Participant participant = new Participant();
        participant.initSocket(new Random().nextInt(500)+1500);
    }

    public void initSocket(int pport) throws IOException {

        //connects to the coordinator
        this.pport = pport;
        try {
            socket = new Socket("127.0.0.1", cport);
            System.out.println("PART: socket connected");
        } catch (IOException e) {
            System.out.println("PART: socket delceration error");
            e.printStackTrace();
        }

        //sets up streams
        writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //requests to join vote
        String temp = new JoinToken(pport).createMessage();
        System.out.println("PART: Sending pport..." + temp);
        writer.println(temp);
        writer.flush();

        //waits for the return of details
        System.out.println("PART: Waiting for reads..");
        Tokenizer tokenizer = new Tokenizer();
        Token token = null;
//        while ((token = tokenizer.getToken(reader.readLine())) != null) {
        token = tokenizer.getToken(reader.readLine());
        while (!(token instanceof DetailsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //adding participants reviced in details
        System.out.println("PART: Participants recived: " + ((DetailsToken) token).getDetailsAsString());
        parts = ((DetailsToken) token).getDetails();

        //allows votes to be collected
        votesRecived  = Collections.synchronizedMap(new HashMap<String, String>(parts.length+1));

        //sets up listner for newly connecting participants and new thread to coordinate them
        PartCoord partCoord = new PartCoord(this, parts, pport);
        Thread partListner = new ParticipantListner(partCoord, pport, parts.length);
        partListner.start();

        //waits for votes
        while (!(token instanceof VoteOptionsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //decides vote
        System.out.println("PART: Voting options received");
        voteOptions = ((VoteOptionsToken) token).getOptions();
        vote = voteOptions[new Random().nextInt(voteOptions.length)];
        System.out.println("PART: Voting for " + vote);

        //add vote to votes record
        votesRecived.put(String.valueOf(pport), vote); //need this??

        //sets the vote in PartCord
        partCoord.setCurrentVoteInit(pport + " " + vote);
        partCoord.startStreams();
        partCoord.broadcastVote();

        //Does nothing until voting finished is called
        System.out.println("PART: waiting...");
    }

    //steps called when voting finished
    public synchronized void votingFinished(String finalVotes){
        System.out.println("PART: Final votes -> " + finalVotes);

        String items[] = finalVotes.split(" ");
        String[] tookPart = new String[items.length/2];
        Map<String, Integer> voteCount = new HashMap<>();
        for (int i = 0; i < items.length; i++){
            if (i%2 == 0){
                tookPart[i/2] = items[i];
            } else {
                if(voteCount.get(items[i]) == null)
                    voteCount.put(items[i], 1);
                else
                    voteCount.put(items[i], voteCount.get(items[i])+1);
            }
        }

        String jointVote = null;
        int max = 0;
        for (String vote : voteCount.keySet()){
            System.out.println("_________ vote" + vote + " coutn " + voteCount.get(vote));
            if (voteCount.get(vote) > max){
                max = voteCount.get(vote);
                jointVote = vote;
            } else if (voteCount.get(vote) == max){
                jointVote = null;
            } else {

            }
        }
        System.out.println("PART: outcome = " + jointVote);

        //returns outcome to coordinator
        System.out.println("PART: Sending outcome...");
        writer.println((new OutcomeToken(jointVote, tookPart)).createMessage());
        writer.flush( );

        System.out.println("PART: closing...");
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
