package hangman;

import db.DataBase;

import java.util.HashMap;
import java.util.Map;

public class HangmanRegistry {

    private static final Map<Long, Hangman> activeHangman = new HashMap<>();
    private static final Map<Long, String> messageId = new HashMap<>();
    private static volatile HangmanRegistry hangmanRegistry;
    private volatile int idGame;

    private HangmanRegistry() {
    }

    public static HangmanRegistry getInstance() {
        if (hangmanRegistry == null) {
            synchronized (HangmanRegistry.class) {
                if (hangmanRegistry == null) {
                    hangmanRegistry = new HangmanRegistry();
                }
            }
        }
        return hangmanRegistry;
    }

    private synchronized void setIdGame() {
        idGame = DataBase.getInstance().getCountGames();
    }

    public synchronized int getIdGame() {
        return idGame = idGame + 1;
    }

    public synchronized void getSetIdGame() {
        setIdGame();
    }

    public Map<Long, Hangman> getActiveHangman() {
        return activeHangman;
    }

    public Map<Long, String> getMessageId() {
        return messageId;
    }

    public void getHangman(long userIdLong) {
        activeHangman.get(userIdLong);
    }

    public void setHangman(long userIdLong, Hangman hangman) {
        activeHangman.put(userIdLong, hangman);
    }

    public boolean hasHangman(long userIdLong) {
        return activeHangman.containsKey(userIdLong);
    }

    public void removeHangman(long userIdLong) {
        activeHangman.remove(userIdLong);
    }

}