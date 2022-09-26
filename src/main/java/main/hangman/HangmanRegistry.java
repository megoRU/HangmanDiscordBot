package main.hangman;

import main.config.BotStartConfig;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HangmanRegistry {
    //Long это UserIdLong
    private static final Map<Long, Hangman> activeHangman = new ConcurrentHashMap<>();
    private static final Map<Long, String> messageId = new ConcurrentHashMap<>();
    private static final Map<Long, Timer> timeCreatedGame = new ConcurrentHashMap<>();
    private static final Map<Long, Timer> timeAutoUpdate = new ConcurrentHashMap<>();
    private static final Map<Long, Timer> autoDeletingMessages = new ConcurrentHashMap<>();

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
    public Timer getAutoDeletingMessages(long userIdLong) {
        return autoDeletingMessages.get(userIdLong);
    }

    public void setAutoDeletingMessages(long userIdLong, Timer timer) {
        autoDeletingMessages.put(userIdLong, timer);
    }

    public Timer getTimeAutoUpdate(long userIdLong) {
        return timeAutoUpdate.get(userIdLong);
    }

    public Timer getHangmanTimer(long userIdLong) {
        return timeCreatedGame.get(userIdLong);
    }

    public void setHangmanTimer(long userIdLong, Timer timer) {
        timeCreatedGame.put(userIdLong, timer);
    }

    public void setTimeAutoUpdate(long userIdLong, Timer timer) {
        timeAutoUpdate.put(userIdLong, timer);
    }

    public int getIdGame() {
        return idGame.incrementAndGet();
    }

    public void setIdGame() {
        idGame.set(BotStartConfig.getIdGame());
        System.out.println(idGame);
    }

    //2 User могут иметь 1 Gift
//    @Nullable TODO: по идеи может Hangman == null так как мы не удаляем 2 пользователя
    public Hangman getActiveHangman(long userIdLong) {
        return activeHangman.get(userIdLong);
    }

    public String getUserConvector(long userIdLong) {
        Hangman hangman = activeHangman.get(userIdLong);
        if (hangman != null) return String.valueOf(hangman.getUserId());
        return String.valueOf(userIdLong);
    }

    @Nullable
    public String getMessageId(long userIdLong) {
        long userConvector = Long.parseLong(getUserConvector(userIdLong));
        return messageId.get(userConvector);
    }

    public void setMessageId(long userIdLong, String messageIdString) {
        messageId.put(userIdLong, messageIdString);
    }

    public void setHangman(long userIdLong, Hangman hangman) {
        activeHangman.put(userIdLong, hangman);
    }

    //2 User могут иметь 1 Gift
    //TODO: по идеи может Hangman == null так как мы не удаляем 2 пользователя
    public boolean hasHangman(long userIdLong) {
        return activeHangman.containsKey(userIdLong);
    }

    public void removeHangman(long userIdLong) {
        Timer timerAutoUpdate = timeAutoUpdate.get(userIdLong);
        Timer timerCreatedGame = timeCreatedGame.get(userIdLong);
        Timer autoDeletingMessage = autoDeletingMessages.get(userIdLong);

        if (timerAutoUpdate != null) {
            timerAutoUpdate.cancel();
            timeAutoUpdate.remove(userIdLong);
        }

        if (timerCreatedGame != null) {
            timerCreatedGame.cancel();
            timeCreatedGame.remove(userIdLong);
        }

        if (autoDeletingMessage != null) {
            autoDeletingMessage.cancel();
            autoDeletingMessages.remove(userIdLong);
        }

        Hangman hangman = activeHangman.get(userIdLong);

        if (hangman.getSecondPlayer() != 0L) {
            Map<Long, Hangman> temp = new ConcurrentHashMap<>(activeHangman);
            temp.entrySet()
                    .stream()
                    .filter(hangmanMap -> hangmanMap.getValue().equals(hangman))
                    .map(hangmanMap -> activeHangman.remove(hangmanMap.getKey()))
                    .collect(Collectors.toList());
        } else {
            activeHangman.remove(userIdLong);
        }
        messageId.remove(userIdLong);
    }
}