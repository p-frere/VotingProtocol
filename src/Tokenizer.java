
enum Command
{
//    JOIN int,
//    DETAILS [int],
//    VOTE_OPTIONS [char],
//    OUTCOME char [int],
//    VOTE;

}




//import java.util.StringTokenizer;
//
///**
// * A scanner and parser for requests.
// */
//class Tokenizer {
//    Tokenizer() { ; }
//
//    public void getToken(String string) {
//        String[] words = string.split(" ");
//
//        switch (words[0]){
//            case "JOIN":
//                break;
//            case "VOTEOPTIONS":
//                break;
//            case "OUTCOME":
//                break;
//            default:
//                System.out.println("corrupt token recived");
//                break;
//    }
//
//
//    /**
//     * Parses requests.
//     */
//    Token getToken(String req) {
//        StringTokenizer sTokenizer = new StringTokenizer(req);
//        if (!(sTokenizer.hasMoreTokens()))
//            return null;
//
//        String firstToken = sTokenizer.nextToken();
//
//        if (firstToken.equals("JOIN")) {
//            if (sTokenizer.hasMoreTokens())
//                return new JoinToken(req, sTokenizer.nextToken());
//            else
//                return null;
//        }
//
//        if (firstToken.equals("YELL")) {
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new YellToken(req, msg);
//        }
//
//        if (firstToken.equals("TELL")) {
//            String name = sTokenizer.nextToken();
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new TellToken(req, name, msg);
//        }
//
//        if (firstToken.equals("OUTCOME")) {
//            String name = sTokenizer.nextToken();
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new TellToken(req, name, msg);
//        }
//
//        if (firstToken.equals("VOTEOPTIONS")) {
//            String name = sTokenizer.nextToken();
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new TellToken(req, name, msg);
//        }
//
//        if (firstToken.equals("DETAILS")) {
//            String name = sTokenizer.nextToken();
//            String msg = "";
//            while (sTokenizer.hasMoreTokens())
//                msg += " " + sTokenizer.nextToken();
//            return new TellToken(req, name, msg);
//        }
//
//        if (firstToken.equals("EXIT"))
//            return new ExitToken(req);
//
//        return null; // Ignore request..
//    }
//}
//
///**
// * The Token Prototype.
// */
//abstract class Token {
//    String _req;
//}
//
///**
// * Syntax: JOIN &lt;name&gt;
// */
//class JoinToken extends Token {
//    String _name;
//    JoinToken(String req, String name) {
//        this._req = req;
//        //turn to int
//        this._name = name;
//    }
//}
//
///**
// * Syntax: YELL &lt;msg&gt;
// */
//class YellToken extends Token {
//    String _msg;
//
//    YellToken(String req, String msg) {
//        this._req = req;
//        this._msg = msg;
//    }
//}
//
///**
// * Syntax: TELL &lt;rcpt&gt; &lt;msg&gt;
// */
//class TellToken extends Token {
//    String _rcpt, _msg;
//
//    TellToken(String req, String rcpt, String msg) {
//        this._req = req;
//        this._rcpt = rcpt;
//        this._msg = msg;
//    }
//}
//
///**
// * Syntax: EXIT
// */
//class ExitToken extends Token {
//    ExitToken(String req) { this._req = req; }
//}
