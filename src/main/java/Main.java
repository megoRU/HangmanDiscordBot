import hangman.HangmanRegistry;
import startbot.BotStart;
import threads.EngGameByTime;
import threads.TopGG;

public class Main {
    public static void main(String[] args) throws Exception {

        HangmanRegistry.getInstance();
        BotStart botStart = new BotStart();
        botStart.startBot();
        new TopGG().runTask();
        new EngGameByTime().runTask();
        System.out.println("19:57");

    }
}
