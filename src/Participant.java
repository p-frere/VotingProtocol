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
        this.pport = pport;


        try {
            socket = new Socket("127.0.0.1", cport);
            System.out.println("socket connected");
        } catch (IOException e) {
            System.out.println("socket delceration error");
            e.printStackTrace();
        }

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String temp = new JoinToken(pport).createMessage();
        System.out.println("Sending pport..." + temp);
        writer.println(temp);
        writer.flush();



        System.out.println("Waiting for reads..");
        Tokenizer tokenizer = new Tokenizer();
        Token token = null;
//        while ((token = tokenizer.getToken(reader.readLine())) != null) {
        token = tokenizer.getToken(reader.readLine());
        while (!(token instanceof DetailsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //adding participants
        System.out.println("Participants recived: " + ((DetailsToken) token).getDetailsAsString());
        parts = ((DetailsToken) token).getDetails();

        //sets up listner for newly connecting participants
        Thread partListner = new ParticipantListner(this, parts, pport);
        partListner.start();
        //allows votes to be collected
        votesRecived  = Collections.synchronizedMap(new HashMap<String, String>(parts.length+1));


        while (!(token instanceof VoteOptionsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //deciding vote
        System.out.println("Voting options received");
        voteOptions = ((VoteOptionsToken) token).getOptions();
        vote = voteOptions[new Random().nextInt(voteOptions.length)];
        System.out.println("Voting for " + vote);

        //add vote to votes record
        votesRecived.put(String.valueOf(pport), vote);


        //wait till done and connected
        //send a vote
        try {
            Socket socket2 = new Socket("127.0.0.1", parts[0]);
            System.out.println("socket2 connected to OTHER PARTICIPANT");

            PrintWriter writer2 = new PrintWriter(socket2.getOutputStream());

            writer2.println(new VoteToken(new String[][]{{String.valueOf(pport), vote}}).createMessage());
            writer2.flush();

            wait();

            //votesRecived.put()



        } catch (IOException e) {
            System.out.println("socket delceration errorto OTHER PARTICIPANT");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        System.out.println("Sending outcome...");
        writer.write(new OutcomeToken(vote, new DetailsToken(parts).getDetailsAsStringArray()).createMessage());
        writer.flush( );

        System.out.println("closing...");
        reader.close();
        writer.close();
        socket.close();

    }


}
