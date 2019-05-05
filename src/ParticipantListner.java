import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ParticipantListner extends Thread {
    Participant participant;
    int[] otherParts;
    int pport;
    int parts;

    //Constructor
    public ParticipantListner(Participant participant, int[] otherParts, int pport) {
        this.participant = participant;
        this.otherParts = otherParts;
        this.pport = pport;
        parts = otherParts.length;
    }

    @Override
    public void run() {

        int joinedCnt = 0;
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(pport);

            System.out.println(pport + " Part listner started");

            while (joinedCnt <= parts) {
                Socket client = listener.accept();
                new ParticipantConnsOUT(client, otherParts).start();
                joinedCnt++;
                System.out.println("connected to part " + joinedCnt + "/" + parts);
            }

            listener.close();
            System.out.println("Stopped part listning.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


