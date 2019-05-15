import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Participant {

    Socket socket = null;
    private int cport;
    private int pport;
    private int timeout;
    private int failurecond;
    private int[] expectedParts;
    private String[] voteOptions;
    private PrintWriter writer;
    private BufferedReader reader;
    private String initialVote;
    private boolean finishedVote;

    public List<BlockingQueue<VoteToken>> getAllQueues() {
        return allQueues;
    }

    public void setAllQueues(List<BlockingQueue<VoteToken>> allQueues) {
        this.allQueues = allQueues;
    }

    private List<BlockingQueue<VoteToken>> allQueues;
    private List<BlockingQueue<VoteToken>> toRemove;
    Runnable partCoord;
    private String debugVote;
    private List<Integer> deadList;

    private CountDownLatch resetCountdown; //size 1, if 0, then reset
    private CountDownLatch votesFinishedCountdown; //if 0 then vote is sumbitted. EITHER O OR 1
    private Integer activeParts;
    //private CountDownLatch roundCountdown; //if zero new round

//    public CountDownLatch getRoundCountdown() {
//        return roundCountdown;
//    }
//
//    public void setRoundCountdown(CountDownLatch roundCountdown) {
//        this.roundCountdown = roundCountdown;
//    }

    public synchronized Integer getActiveParts() {
        return activeParts;
    }

    public synchronized void setActiveParts(Integer activeParts) {
        this.activeParts = activeParts;
    }

    public CountDownLatch getVotesFinishedCountdown() {
        return votesFinishedCountdown;
    }

    public synchronized CountDownLatch getResetCountdown() {
        return resetCountdown;
    }

    public synchronized void setResetCountdown(CountDownLatch resetCountdown) {
        this.resetCountdown = resetCountdown;
    }

    public List<Integer> getDeadList() {
        return deadList;
    }

    public void setDeadList(List<Integer> deadList) {
        this.deadList = deadList;
    }



    public List<BlockingQueue<VoteToken>> getToRemove() {
        return toRemove;
    }
    public synchronized int[] getExpectedParts() {
        return expectedParts;
    }
    public synchronized void setExpectedParts(int[] expectedParts) {
        this.expectedParts = expectedParts;
    }
    public synchronized String getInitialVote() {
        return initialVote;
    }
    public synchronized void setInitialVote(String initialVote) {
        this.initialVote = initialVote;
    }
    public synchronized boolean isFinishedVote() {
        return finishedVote;
    }
    public synchronized void setFinishedVote(boolean finishedVote, String currentVote) {
        this.finishedVote = finishedVote;
        setInitialVote(currentVote);
    }


    public static void main(String[] args) throws Exception {
        System.out.println("PART: Started");
        new Participant(args);
    }

    public Participant(String args[]) throws Exception {
        init(args);
        initSocket();
        initCoordComm();
        votesFinishedCountdown.await();
        System.out.println("PART VOTES FINISHED");
        calculateVotes();
        reset();
        close();
    }


    private void reset() throws IOException, InterruptedException {
        String temp = null;
        try {
            while ((temp = reader.readLine()) != null) {
                System.out.println("PART: read a reset");
                votesFinishedCountdown = new CountDownLatch(1);
                allQueues = Collections.synchronizedList(new ArrayList<BlockingQueue<VoteToken>>());
                setFinishedVote(false, "");
                setInitialVote(new VoteToken(String.valueOf(pport) + " " + voteOptions[new Random().nextInt(voteOptions.length)]).getVotesAsString());
                System.out.println("PART: Voting for " + getInitialVote());
                //roundCountdown = new CountDownLatch(getActiveParts());

                System.out.println("PART: ----------------->" + resetCountdown.getCount());
                resetCountdown.countDown();
                System.out.println("PART: ----------------->" + resetCountdown.getCount());
                votesFinishedCountdown.await();
                calculateVotes();
                reset();
            }
        } catch (IOException e) {
            System.out.println("PART: timeout, no reset read");
        }
    }

    private void init(String[] args) throws Exception{
        this.cport = Integer.parseInt(args[0]);
        this.pport = Integer.parseInt(args[1]);
        //this.pport = new Random().nextInt(500)+1500;
        this.timeout = Integer.parseInt(args[2]);
        this.failurecond = Integer.parseInt(args[3]);
        //this.debugVote = args[4];

        resetCountdown = new CountDownLatch(1);
        deadList = Collections.synchronizedList(new ArrayList<Integer>());
        toRemove = Collections.synchronizedList(new ArrayList<BlockingQueue<VoteToken>>());
        allQueues = Collections.synchronizedList(new ArrayList<BlockingQueue<VoteToken>>());
        partCoord = new PartCoord(pport, allQueues, this, timeout, failurecond, toRemove);
        Runnable partListner = new ParticipantListner(pport, allQueues, this);
        Thread partListnerThread = new Thread(partListner);
        partListnerThread.start();
    }

    public void initSocket() throws IOException {

        //connects to the coordinator
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
    }



    //----------------Starts listner
    private void initCoordComm() throws IOException {

        //requests to join vote
        String temp = new JoinToken(pport).createMessage();
        System.out.println("PART: Sending pport..." + temp);
        writer.println(temp);
        writer.flush();

        //waits for the return of details
        System.out.println("PART: Waiting for reads..");
        Tokenizer tokenizer = new Tokenizer();
        Token token = null;
        token = tokenizer.getToken(reader.readLine());
        while (!(token instanceof DetailsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //adding participants reviced in details
        System.out.println("PART: Participants recived: " + ((DetailsToken) token).getDetailsAsString());
        setExpectedParts(((DetailsToken) token).getDetails());

        //Further set up after more details recived
        setActiveParts(expectedParts.length);
        votesFinishedCountdown = new CountDownLatch(1);
        //roundCountdown = new CountDownLatch(getActiveParts());

        //waits for votes
        while (!(token instanceof VoteOptionsToken)) {
            token = tokenizer.getToken(reader.readLine());

        }

        //decides vote and sets it
        System.out.println("PART: Voting options received");
        voteOptions = ((VoteOptionsToken) token).getOptions();
        setInitialVote(new VoteToken(String.valueOf(pport) + " " + voteOptions[new Random().nextInt(voteOptions.length)]).getVotesAsString());
        System.out.println("PART: Voting for " + getInitialVote());


        //sets the vote in PartCord
        Thread partCoordThread = new Thread(partCoord);
        partCoordThread.start();

    }


    private void calculateVotes() {

        System.out.println("PART: Final votes -> " + initialVote);

        //calculated all who voted
        String items[] = initialVote.split(" ");
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

        //calculating most voted
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
    }

    public void close(){
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
