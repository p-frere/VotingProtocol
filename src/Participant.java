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
    private DataInputStream  input   = null;
    private DataOutputStream out     = null;
    private Map<String, String> votesRecived;

    public static void main(String[] args) throws Exception {
        System.out.println("Started");

        Participant participant = new Participant();
        participant.initSocket(new Random().nextInt(500)+1500);
    }

    public void initSocket(int pport) throws IOException {

        //connects to the coordinator
        this.pport = pport;
        try {
            socket = new Socket("127.0.0.1", cport);
            System.out.println("socket connected");
        } catch (IOException e) {
            System.out.println("socket delceration error");
            e.printStackTrace();
        }

        //sets up streams
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //requests to join vote
        String temp = new JoinToken(pport).createMessage();
        System.out.println("Sending pport..." + temp);
        writer.println(temp);
        writer.flush();

        //waits for the return of details
        System.out.println("Waiting for reads..");
        Tokenizer tokenizer = new Tokenizer();
        Token token = null;
//        while ((token = tokenizer.getToken(reader.readLine())) != null) {
        token = tokenizer.getToken(reader.readLine());
        while (!(token instanceof DetailsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //adding participants reviced in details
        System.out.println("Participants recived: " + ((DetailsToken) token).getDetailsAsString());
        parts = ((DetailsToken) token).getDetails();

        //allows votes to be collected
        votesRecived  = Collections.synchronizedMap(new HashMap<String, String>(parts.length+1));

        //sets up listner for newly connecting participants and new thread to coordinate them
        PartCoord partCoord = new PartCoord(this, parts);
        Thread partListner = new ParticipantListner(partCoord, pport, parts.length);
        partListner.start();

        //waits for votes
        while (!(token instanceof VoteOptionsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //decides vote
        System.out.println("Voting options received");
        voteOptions = ((VoteOptionsToken) token).getOptions();
        vote = voteOptions[new Random().nextInt(voteOptions.length)];
        System.out.println("Voting for " + vote);

        //add vote to votes record
        votesRecived.put(String.valueOf(pport), vote); //need this??

        //sets the vote in PartCord
        partCoord.setCurrentVote(pport + " " + vote);
        partCoord.startStreams();
        partCoord.broadcastVote();

        //wait till done and connected
        //send a vote
//        try {
//            Socket socket2 = new Socket("127.0.0.1", parts[0]);
//            System.out.println("socket2 connected to OTHER PARTICIPANT");
//
//            PrintWriter writer2 = new PrintWriter(socket2.getOutputStream());
//
//            writer2.println(new VoteToken(new String[][]{{String.valueOf(pport), vote}}).createMessage());
//            writer2.flush();
//
//            wait();
//
//
//
//
//        } catch (IOException e) {
//            System.out.println("socket delceration errorto OTHER PARTICIPANT");
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//
//        //returns outcome to coordinator
//        System.out.println("Sending outcome...");
//        writer.write(new OutcomeToken(vote, new DetailsToken(parts).getDetailsAsStringArray()).createMessage());
//        writer.flush( );

        //wait
        try {
            System.out.println("waiting...");
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("closing...");
        reader.close();
        writer.close();
        socket.close();

    }

    public synchronized void addVote(String id, String votes){
        votesRecived.put(id,votes);
        System.out.println("recived " + votesRecived.size() + "/" + parts.length + " votes");

        if(votesRecived.size() == parts.length){

        }
    }


}
