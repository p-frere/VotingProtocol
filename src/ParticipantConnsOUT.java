import java.io.*;
import java.net.Socket;

class ParticipantConnsOUT extends Thread {

    BufferedReader reader;
    PrintWriter writer;
    Socket partSocket;
    int[] otherParts;

    public ParticipantConnsOUT(Socket partSocket, int[] otherParts){
        this.partSocket = partSocket;
        this.otherParts = otherParts;
        System.out.println("new thread created for voting");
    }


    @Override
    public void run() {
    // Open I/O steams
        try {


        reader = new BufferedReader( new InputStreamReader( partSocket.getInputStream() ) );
        writer = new PrintWriter( new OutputStreamWriter( partSocket.getOutputStream() ) );

//                // Welcome message.
//                writer.println( "HI");
//                writer.flush();

        //partSocket.setSoTimeout(timeout);

        Token token = null;

        Tokenizer tokenizer = new Tokenizer();
        System.out.println("reading from connected parts...");
        // First, the part must send a join


        String temp = reader.readLine();
        token = tokenizer.getToken(temp);

        if (token instanceof VoteToken) {

            System.out.println("First token not a join token");

        }

            System.out.println("BICH");



//            for(int port : otherParts) {
//                Socket socket = new Socket("127.0.0.1", port);
//                System.out.println("socket connected");
//
//                reader = new BufferedReader(new InputStreamReader(partSocket.getInputStream()));
//                writer = new PrintWriter(new OutputStreamWriter(partSocket.getOutputStream()));
//
//                //  Welcome message.
//                writer.println("*** You have been connected ***");
//                writer.flush();
//
//                reader.close();
//                writer.close();
//                partSocket.close();
//            }


        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
