import Hangman.HangmanRegistry;
import startbot.BotStart;
import threads.TopGG;

public class Main {
    public static void main(String[] args) throws Exception {

        BotStart botStart = new BotStart();
        botStart.startBot();
        HangmanRegistry.getInstance();
        new TopGG().start();
    }
}
