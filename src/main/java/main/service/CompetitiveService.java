package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.Hangman;
import main.game.HangmanBuilder;
import main.game.HangmanDataSaving;
import main.game.HangmanPlayer;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class CompetitiveService {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanAPI hangmanAPI;

    private final static Logger LOGGER = LoggerFactory.getLogger(CompetitiveService.class.getName());

    public void startGame(JDA jda) {
        HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();
        int competitiveQueueSize = hangmanRegistry.getCompetitiveQueueSize();
        if (competitiveQueueSize > 1) {
            HangmanPlayer[] competitivePlayers = hangmanRegistry.getCompetitivePlayers();
            if (competitivePlayers.length > 1) {
                long userId = competitivePlayers[0].getUserId();

                try {
                    String word = hangmanAPI.getWord(userId);
                    for (HangmanPlayer competitiveCurrentPlayer : competitivePlayers) {
                        long currentPlayerUserId = competitiveCurrentPlayer.getUserId();

                        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
                        UserSettings userSettings = userSettingsMap.get(userId);

                        if (userSettings == null) {
                            competitiveQueueRepository.deleteById(currentPlayerUserId);
                            hangmanRegistry.removeFromCompetitiveQueue(currentPlayerUserId);
                            return;
                        }

                        long anotherUserId = getAnotherUserId(currentPlayerUserId, competitivePlayers);

                        HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
                        hangmanBuilder.addHangmanPlayer(competitiveCurrentPlayer);
                        hangmanBuilder.setCompetitive(true);
                        hangmanBuilder.setAgainstPlayerId(anotherUserId);

                        Hangman hangman = hangmanBuilder.build();
                        HangmanRegistry.getInstance().setHangman(currentPlayerUserId, hangman);

                        jda.retrieveUserById(currentPlayerUserId)
                                .queue(user -> user.openPrivateChannel()
                                                .queue(channel -> hangman.startGame(channel, word, hangmanDataSaving),
                                                        throwable -> LOGGER.error(throwable.getMessage(), throwable)),
                                        throwable -> LOGGER.error(throwable.getMessage(), throwable));

                        //Удаляем из очереди
                        competitiveQueueRepository.deleteById(currentPlayerUserId);
                        hangmanRegistry.removeFromCompetitiveQueue(currentPlayerUserId);
                    }
                } catch (Exception e) {
                    jda.retrieveUserById(userId)
                            .queue(user -> user.openPrivateChannel()
                                            .queue(channel -> HangmanUtils.handleAPIException(userId, channel),
                                                    throwable -> LOGGER.error(throwable.getMessage(), throwable)),
                                    throwable -> LOGGER.error(throwable.getMessage(), throwable));
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    private long getAnotherUserId(long currentUserId, HangmanPlayer[] competitivePlayers) {
        for (HangmanPlayer competitivePlayer : competitivePlayers) {
            long localUserId = competitivePlayer.getUserId();
            if (localUserId != currentUserId) return localUserId;
        }
        throw new IllegalArgumentException();
    }
}