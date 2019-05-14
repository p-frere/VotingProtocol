import java.util.Arrays;

enum TokenType
{
    JOIN,
    DETAILS,
    VOTE_OPTIONS,
    OUTCOME,
    VOTE,
    RESTART

//    JOIN int,
//    DETAILS [int],
//    VOTE_OPTIONS [char],
//    OUTCOME char [int],
//    VOTE [[int]];

}

/**
 * A scanner and parser for requests.
 */
class Tokenizer {

    public Token getToken(String string) {
        //System.out.println("TOK: recived: " + string);
        String[] words = string.split(" ");

        switch (words[0]) {
            case "JOIN":
                //check for words
                return new JoinToken(words[1]);
            case "VOTE_OPTIONS":
                return new VoteOptionsToken(Arrays.copyOfRange(words, 1, words.length));
            case "OUTCOME":
                return new OutcomeToken(words[1], Arrays.copyOfRange(words, 2, words.length));
            case "VOTE":
                return new VoteToken(Arrays.copyOfRange(words, 1, words.length));
            case "DETAILS":
                return new DetailsToken(Arrays.copyOfRange(words, 1, words.length));
            case "RESTART":
                return new RestartToken();
            default:
                System.out.println("TOK: corrupt token received");
                return null;
        }
    }
}


abstract class Token {
    TokenType tokenType;
    abstract String createMessage();
}

class JoinToken extends Token {
    int pport;

    JoinToken(Integer pport) {
        tokenType = TokenType.JOIN;
        this.pport = pport;
    }

    JoinToken(String name) {
        tokenType = TokenType.JOIN;
        this.pport = Integer.valueOf(name);
    }


    public String getPportAsString() {
        return Integer.toString(pport);
    }

    public int getPport() {
        return pport;
    }

    public String createMessage(){
        return  (tokenType.toString() + " " + getPportAsString());
    }
}


class RestartToken extends Token {
    RestartToken() {
        tokenType = TokenType.RESTART;
    }
    public String createMessage(){
        return  (tokenType.toString());
    }
}

class OutcomeToken extends Token {
    private String[] parts;
    private String outcome;

    OutcomeToken(String outcome, String[] parts) {
        tokenType = TokenType.OUTCOME;
        this.outcome = outcome;
        this.parts = parts;
    }

    public String getOutcome() {
        return outcome;
    }

    public String[] getParts() {
        return parts;
    }

    public String getPartsAsString(){
        StringBuilder sb = new StringBuilder();
        for (String part : parts){
            sb.append(part);
            sb.append(" ");
        }
        //sb.substring(0, sb.length()-2);
        return sb.toString();
    }

    public String createMessage(){
        return  (tokenType.toString() + " " + getOutcome() + " " + getPartsAsString());
    }
}

//-------Details----
class DetailsToken extends Token {
    private int[] details;

    DetailsToken(int[] details) {
        tokenType = TokenType.DETAILS;
        this.details = details;
    }

    DetailsToken(String[] details) {
        tokenType = TokenType.DETAILS;
        //converts string array to int array
        this.details =  Arrays.asList(details).stream().mapToInt(Integer::parseInt).toArray();
    }

    public int[] getDetails() {
        return details;
    }

    public String getDetailsAsString(){
        StringBuilder sb = new StringBuilder();
        for (int deet : details){
            sb.append(deet);
            sb.append(" ");
        }
        //sb.substring(0, sb.length()-2);
        return sb.toString();
    }

    public String[] getDetailsAsStringArray() {
        return Arrays.stream(details).mapToObj(String::valueOf).toArray(String[]::new);
    }

    public String createMessage(){
        return  (tokenType.toString() + " "  + getDetailsAsString());
    }
}

//--------Vote Options
class VoteOptionsToken extends Token {
    private String[] options;

    VoteOptionsToken(String[] options) {
        tokenType = TokenType.VOTE_OPTIONS;
        this.options = options;
    }

    public String[] getOptions() {
        return options;
    }

    public String getOptionsAsString(){
        StringBuilder sb = new StringBuilder();
        for (String part : options){
            sb.append(part);
            sb.append(" ");
        }
        //sb.substring(0, sb.length()-2);
        return sb.toString();
    }

    public String createMessage(){
        return  (tokenType.toString() + " "  + getOptionsAsString());
    }
}

//-----------vote
class VoteToken extends Token {
    private String[][] votes;
    private String voteAsSting;

    VoteToken(String[][] votes) {
        tokenType = TokenType.VOTE;
        this.votes = votes;
        voteAsSting = null;
    }

    VoteToken(String votes) {
        tokenType = TokenType.VOTE;
        this.voteAsSting = votes;
        this.votes = makeArray(votes.split(" "));
    }

    VoteToken(String[] votes) {
        tokenType = TokenType.VOTE;
        this.votes = makeArray(votes);
        voteAsSting = null;
    }

    private String[][] makeArray(String votes[]){
        String[][] orderedVotes = new String[votes.length/2][2];
        for (int i = 0; i < votes.length; i+=2){
            orderedVotes[i/2][0] = votes[i];
            orderedVotes[i/2][1] = votes[i+1];
        }
        return orderedVotes;
    }


    public String[][] getVotes() {
        return votes;
    }

    public String getVotesAsString(){
        if (voteAsSting != null){
            return voteAsSting;
        }

        StringBuilder sb = new StringBuilder();
        for (String[] vote : votes){
            sb.append(vote[0]);
            sb.append(" ");
            sb.append(vote[1]);
            sb.append(" ");
        }
        //sb.substring(0, sb.length()-2);
        return sb.toString();
    }

    public String createMessage(){
            return  (tokenType.toString() + " "  + getVotesAsString());
    }
}