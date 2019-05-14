import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParticipantListner extends Thread {
    int pport;
    List<BlockingQueue<VoteToken>> allQueues;
    Participant participant;

    //Constructor
    public ParticipantListner(int pport, List<BlockingQueue<VoteToken>> allQueues, Participant participant) {
        this.allQueues = allQueues;
        this.pport = pport;
        this.participant = participant;
    }

    @Override
    public void run() {

        int joinedCnt = 0;
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(pport);
            System.out.println("LIS: " + pport + " Part listner started");

            //accept new connections, create a new thread for them
            while (true) {
                Socket client = listener.accept();
                BlockingQueue<VoteToken> votesRecived = new LinkedBlockingQueue<>();
                allQueues.add(votesRecived);
                new ParticipantConnsOUT(client, votesRecived, participant).start();
                joinedCnt++;
                System.out.println("LIS: connected to part " + joinedCnt);
            }

            //close when expected number of connections
            //listener.close();
            //System.out.println("LIS: Stopped part listning.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


