import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ParticipantListner extends Thread {
    PartCoord partCoord;
    int pport;
    int initPartCnt;

    //Constructor
    public ParticipantListner(PartCoord partCoord, int pport, int initPartCnt) {
        this.partCoord = partCoord;
        this.pport = pport;
        this.initPartCnt = initPartCnt;
    }

    @Override
    public void run() {

        int joinedCnt = 0;
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(pport);
            System.out.println(pport + " Part listner started");

            //accept new connections, create a new thread for them
            while (joinedCnt <= initPartCnt) {
                Socket client = listener.accept();
                new ParticipantConnsOUT(client, partCoord).start();
                joinedCnt++;
                System.out.println("connected to part " + joinedCnt + "/" + initPartCnt);
            }

            //close when expected number of connections
            listener.close();
            System.out.println("Stopped part listning.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


