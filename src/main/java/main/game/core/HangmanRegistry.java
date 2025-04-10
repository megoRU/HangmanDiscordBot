package main.game.core;

import main.enums.GameStatus;
import main.game.Hangman;
import main.game.HangmanPlayer;
import main.model.entity.UserSettings;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HangmanRegistry {

    //Long это UserIdLong
    private static final ConcurrentMap<Long, Hangman> activeHangman = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Hangman, Timestamp> hangmanTimer = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, HangmanPlayer> competitiveQueue = new ConcurrentHashMap<>();
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

    public void setHangmanStatus(long userIdLong, GameStatus gameStatus) {
        Hangman hangman = activeHangman.get(userIdLong);
        if (hangman != null) {
            hangman.setGameStatus(gameStatus);
        }
    }

    public void setHangmanTimer(Hangman hangman, Timestamp timestamp) {
        hangmanTimer.put(hangman, timestamp);
    }

    public void addCompetitiveQueue(HangmanPlayer hangmanPlayer) {
        competitiveQueue.put(hangmanPlayer.getUserId(), hangmanPlayer);
    }

    public boolean hasCompetitive(long userIdLong) {
        HangmanPlayer hangmanPlayer = competitiveQueue.get(userIdLong);
        return hangmanPlayer != null;
    }

    @Nullable
    public Hangman isMessageIdHas(long messageId) {
        Collection<Hangman> allGames = getAllGames();
        for (Hangman hangman : allGames) {
            if (hangman != null) {
                Long hangmanMessageId = hangman.getMessageId();
                if (hangmanMessageId != null && hangmanMessageId == messageId) {
                    return hangman;
                }
            }
        }
        return null;
    }

    public int getCompetitiveQueueSize() {
        return competitiveQueue.size();
    }

    public List<HangmanPlayer> getCompetitiveQueue() {
        return new ArrayList<>(competitiveQueue.values());
    }

    public void removeFromCompetitiveQueue(long userIdLong) {
        competitiveQueue.remove(userIdLong);
    }

    public HangmanPlayer[] getCompetitivePlayers() {
        if (getCompetitiveQueueSize() > 1) {
            return findPlayersWithSameLanguage();
        } else {
            return new HangmanPlayer[]{};
        }
    }

    private HangmanPlayer[] findPlayersWithSameLanguage() {
        List<HangmanPlayer> listEnglish = competitiveQueue
                .values()
                .stream()
                .filter(v -> v.getGameLanguage() == UserSettings.GameLanguage.EN)
                .toList();

        List<HangmanPlayer> listRussian = competitiveQueue
                .values()
                .stream()
                .filter(v -> v.getGameLanguage() == UserSettings.GameLanguage.RU)
                .toList();

        if (listEnglish.size() > 1) {
            return new HangmanPlayer[]{listEnglish.get(0), listEnglish.get(1)};
        } else if (listRussian.size() > 1) {
            return new HangmanPlayer[]{listRussian.get(0), listRussian.get(1)};
        } else {
            return new HangmanPlayer[]{};
        }
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
        if (hangman.isCompetitive()) {
            if (hangman.getGameStatus().equals(GameStatus.WIN_GAME)) {
                Long againstPlayerId = hangman.getAgainstPlayerId();
                if (againstPlayerId != null) {
                    activeHangman.remove(hangman.getAgainstPlayerId());
                }
            }
        }
        for (HangmanPlayer hangmanPlayer : hangmanPlayers) {
            activeHangman.remove(hangmanPlayer.getUserId());
        }

    }
}