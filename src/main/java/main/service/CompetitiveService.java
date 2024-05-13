package main.service;

import lombok.AllArgsConstructor;
import main.game.*;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class CompetitiveService {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;
    private final HangmanAPI hangmanAPI;
    private final UserSettingsService userSettingsService;

    private static final Logger LOGGER = Logger.getLogger(CompetitiveService.class.getName());

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
                        UserSettings.GameLanguage gameLanguage = userSettingsService.getUserGameLanguage(currentPlayerUserId);

                        if (gameLanguage == null) {
                            competitiveQueueRepository.deleteById(currentPlayerUserId);
                            hangmanRegistry.removeFromCompetitiveQueue(currentPlayerUserId);
                            return;
                        }

                        HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
                        hangmanBuilder.addHangmanPlayer(competitiveCurrentPlayer);
                        hangmanBuilder.setCompetitive(true);
                        hangmanBuilder.setAgainstPlayerId(getAnotherUserId(currentPlayerUserId, competitivePlayers));
                        hangmanBuilder.setHangmanGameRepository(hangmanGameRepository);
                        hangmanBuilder.setHangmanDataSaving(hangmanDataSaving);
                        hangmanBuilder.setHangmanResult(hangmanResult);

                        Hangman hangman = hangmanBuilder.build();
                        HangmanRegistry.getInstance().setHangman(currentPlayerUserId, hangman);

                        CacheRestAction<PrivateChannel> privateChannelCacheRestAction = jda
                                .retrieveUserById(currentPlayerUserId)
                                .complete()
                                .openPrivateChannel();
                        PrivateChannel complete = privateChannelCacheRestAction.complete();
                        hangman.startGame(complete, word);

                        //Удаляем из очереди
                        competitiveQueueRepository.deleteById(currentPlayerUserId);
                        hangmanRegistry.removeFromCompetitiveQueue(currentPlayerUserId);
                    }
                } catch (Exception e) {
                    PrivateChannel privateChannel = jda
                            .retrieveUserById(userId)
                            .complete()
                            .openPrivateChannel()
                            .complete();
                    HangmanUtils.handleAPIException(userId, privateChannel);
                    LOGGER.log(Level.SEVERE, e.getMessage());
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