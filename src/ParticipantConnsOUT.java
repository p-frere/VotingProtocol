import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ParticipantConnsOUT extends Thread {

    BufferedReader reader;
    PrintWriter writer;
    Socket partSocket;
    PartCoord partCoord;


    public ParticipantConnsOUT(Socket partSocket, PartCoord partCoord){
        this.partSocket = partSocket;
        System.out.println("CONS: new thread created for voting");
        this.partCoord = partCoord;
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
                    partCoord.addVote(((VoteToken) token).getVotes()[0][0], ((VoteToken) token).getVotesAsString());
                } else {
                    System.out.println("CONS: not a vote token");
                }
            }

        } catch (IOException e) {
            //if connection dies, send alert
            partCoord.removeParticipant();
            e.printStackTrace();
        }


    }


}
