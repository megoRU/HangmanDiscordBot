package main.hangman;

import main.config.BotStartConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HangmanRegistry {

    //Long это UserIdLong
    private static final Map<Long, Hangman> activeHangman = new ConcurrentHashMap<>();
    private static volatile HangmanRegistry hangmanRegistry;
    private final AtomicInteger idGame = new AtomicInteger();

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

    //2 User могут иметь 1 game
    @Nullable
    public Hangman getActiveHangman(long userIdLong) {
        return activeHangman.get(userIdLong);
    }

    public void setHangman(long userIdLong, Hangman hangman) {
        activeHangman.put(userIdLong, hangman);
    }

    //2 User могут иметь 1 Gift
    public boolean hasHangman(long userIdLong) {
        return activeHangman.containsKey(userIdLong);
    }

    public void removeHangman(long userIdLong) {
        Hangman hangman = activeHangman.get(userIdLong);
        if (hangman == null) return;

        Timer timerAutoInsert = hangman.getAutoInsert();
        Timer stopHangmanTimer = hangman.getStopHangmanTimer();
        Timer autoDeletingMessage = hangman.getAutoDeletingTimer();

        if (timerAutoInsert != null) {
            timerAutoInsert.cancel();
        }

        if (stopHangmanTimer != null) {
            stopHangmanTimer.cancel();
        }

        if (autoDeletingMessage != null) {
            autoDeletingMessage.cancel();
        }

        if (hangman.getSecondPlayer() != 0L) {
            activeHangman.remove(hangman.getSecondPlayer());
        }

        activeHangman.remove(userIdLong);
    }
}