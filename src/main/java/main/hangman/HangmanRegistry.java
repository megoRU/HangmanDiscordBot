package main.hangman;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class HangmanRegistry {
    //Long это UserIdLong
    private static final Map<Long, Hangman> activeHangman = new HashMap<>();
    private static final Map<Long, String> messageId = new HashMap<>();
    private static final Map<Long, LocalDateTime> timeCreatedGame = new HashMap<>();
    private static final Map<Long, String> endAutoDelete = new HashMap<>();
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

    public Map<Long, LocalDateTime> getTimeCreatedGame() {
        return timeCreatedGame;
    }

    public Map<Long, String> getEndAutoDelete() {
        return endAutoDelete;
    }

    private synchronized void setIdGame() {
        //TODO: Сделать через репозитории

//        idGame = DataBase.getInstance().getCountGames();
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
