package main.hangman;

import main.config.BotStartConfig;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HangmanRegistry {

    //Long это UserIdLong
    private static final ConcurrentMap<Long, Hangman> activeHangman = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Hangman, Timestamp> hangmanTimer = new ConcurrentHashMap<>();

    private final AtomicInteger idGame = new AtomicInteger();

    private static volatile HangmanRegistry hangmanRegistry;

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

    public int getIdGame() {
        return idGame.incrementAndGet();
    }

    public void setIdGame() {
        idGame.set(BotStartConfig.getIdGame());
        System.out.println(idGame);
    }

    public Collection<Hangman> getAllGames() {
        return activeHangman.values();
    }

    //2 User могут иметь 1 game
    @Nullable
    public Hangman getActiveHangman(long userIdLong) {
        return activeHangman.get(userIdLong);
    }

    public void setHangman(long userIdLong, Hangman hangman) {
        activeHangman.put(userIdLong, hangman);
    }

    public void setHangmanTimer(Hangman hangman, Timestamp timestamp) {
        hangmanTimer.put(hangman, timestamp);
    }

    public Timestamp getHangmanTimer(Hangman hangman) {
        return hangmanTimer.get(hangman);
    }

    //2 User могут иметь 1 Gift
    public boolean hasHangman(long userIdLong) {
        return activeHangman.containsKey(userIdLong);
    }

    public void removeHangman(long userIdLong) {
        Hangman hangman = getActiveHangman(userIdLong);
        if (hangman == null) return;
        hangmanTimer.remove(hangman);
        HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
        for (HangmanPlayer hangmanPlayer : hangmanPlayers) {
            activeHangman.remove(hangmanPlayer.getUserId());
        }
    }
}