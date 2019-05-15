import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

class ParticipantConnsOUT extends Thread {

    BufferedReader reader;
    PrintWriter writer;
    Socket partSocket;
    BlockingQueue<VoteToken> votesRecived;
    Participant participant;
    private CountDownLatch roundCountdown;
    private List<Integer> deadList;
    List<BlockingQueue<VoteToken>> allQueues;
    int index;

    public ParticipantConnsOUT(Socket partSocket, BlockingQueue<VoteToken> votesRecived, Participant participant, int index){
        this.partSocket = partSocket;
        //System.out.println("CONS: new thread created for voting");
        this.votesRecived = votesRecived;
        this.participant = participant;
        this.deadList = participant.getDeadList();
        this.allQueues = participant.getAllQueues();
        this.index = index;
        //System.out.println("CONNS: index is " + index);
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
            //System.out.println("CONS: reading from connected parts...");

            String temp = null;


            while (true) {
                temp = reader.readLine();
                if (temp == null){
                    //System.out.println("CONNS: Null read");
                    throw new IOException();
                } else {
                    token = tokenizer.getToken(temp);

                    if (token instanceof VoteToken) {
                        //send voter id (first id in list) and votes to the participant coordinator
                        //partCoord.addVote(((VoteToken) token).getVotes()[0][0], ((VoteToken) token).getVotesAsString());
                        votesRecived.add((VoteToken) token);
                        //TODO wait on this?
                        //participant.getRoundCountdown().countDown();
                    } else {
                        System.out.println("CONS: not a vote token");
                    }
                }
            }

        } catch (IOException e) {
            //if connection dies, send alert
            //participant.getToRemove().add(votesRecived);
            deadList.add(index);
            int ap = participant.getActiveParts();
            participant.setActiveParts(ap);
            System.out.println("CONNS: A participant has died");

            //votesRecived.add(new VoteToken(null)); //ADD this?


            //participant.getRoundCountdown().countDown();

            //allQueues.remove(votesRecived);
            //e.printStackTrace();
        }


    }


}
