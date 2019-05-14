import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;

class ParticipantConnsOUT extends Thread {

    BufferedReader reader;
    PrintWriter writer;
    Socket partSocket;
    BlockingQueue<VoteToken> votesRecived;
    Participant participant;


    public ParticipantConnsOUT(Socket partSocket, BlockingQueue<VoteToken> votesRecived, Participant participant){
        this.partSocket = partSocket;
        System.out.println("CONS: new thread created for voting");
        this.votesRecived = votesRecived;
        this.participant = participant;
    }


    @Override
    public void run() {
    // Open I/O steams
        try {
            reader = new BufferedReader( new InputStreamReader( partSocket.getInputStream() ) );
            writer = new PrintWriter( new OutputStreamWriter( partSocket.getOutputStream() ) );

            //partSocket.setSoTimeout(timeout);

            Token token = null;
            Tokenizer tokenizer = new Tokenizer();
            System.out.println("CONS: reading from connected parts...");

            String temp = null;
            while ((temp = reader.readLine()) != null) {
                token = tokenizer.getToken(temp);

                if (token instanceof VoteToken) {
                    //send voter id (first id in list) and votes to the participant coordinator
                    //partCoord.addVote(((VoteToken) token).getVotes()[0][0], ((VoteToken) token).getVotesAsString());
                    votesRecived.add((VoteToken)token);
                } else {
                    System.out.println("CONS: not a vote token");
                }
            }

        } catch (IOException e) {
            //if connection dies, send alert
            //TODO what to do here
            System.out.println("!!!CONS: Caught I/O Exception!!!");
            participant.getToRemove().add(votesRecived);
            //allQueues.remove(votesRecived);
            //e.printStackTrace();
        }


    }


}
