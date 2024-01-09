package main.service;

import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.game.*;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.repository.CompetitiveQueueRepository;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CompetitiveService {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final UpdateController updateController;
    private final HangmanResult hangmanResult;
    private final HangmanAPI hangmanAPI;

    private static final Logger LOGGER = Logger.getLogger(CompetitiveService.class.getName());

    @Autowired
    public CompetitiveService(CompetitiveQueueRepository competitiveQueueRepository,
                              HangmanGameRepository hangmanGameRepository,
                              HangmanDataSaving hangmanDataSaving,
                              UpdateController updateController,
                              HangmanResult hangmanResult) {
        this.competitiveQueueRepository = competitiveQueueRepository;
        this.hangmanGameRepository = hangmanGameRepository;
        this.hangmanDataSaving = hangmanDataSaving;
        this.updateController = updateController;
        this.hangmanResult = hangmanResult;
        this.hangmanAPI = new HangmanAPI();
    }

    public void startGame() {
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

                        HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
                        hangmanBuilder.addHangmanPlayer(competitiveCurrentPlayer);
                        hangmanBuilder.setCompetitive(true);
                        hangmanBuilder.setAgainstPlayerId(getAnotherUserId(currentPlayerUserId, competitivePlayers));
                        hangmanBuilder.setUpdateController(updateController);
                        hangmanBuilder.setHangmanGameRepository(hangmanGameRepository);
                        hangmanBuilder.setHangmanDataSaving(hangmanDataSaving);
                        hangmanBuilder.setHangmanResult(hangmanResult);

                        Hangman hangman = hangmanBuilder.build();
                        HangmanRegistry.getInstance().setHangman(currentPlayerUserId, hangman);

                        CacheRestAction<PrivateChannel> privateChannelCacheRestAction = BotStartConfig
                                .jda
                                .retrieveUserById(currentPlayerUserId)
                                .complete()
                                .openPrivateChannel();
                        PrivateChannel complete = privateChannelCacheRestAction.complete();
                        hangman.startGame(complete, word);
                    }
                } catch (Exception e) {
                    PrivateChannel privateChannel = BotStartConfig
                            .jda
                            .retrieveUserById(userId)
                            .complete()
                            .openPrivateChannel()
                            .complete();
                    HangmanUtils.handleAPIException(userId, privateChannel);
                    LOGGER.log(Level.SEVERE, e.getMessage());
                }

                competitiveQueueRepository.deleteById(competitivePlayers[0].getUserId());
                competitiveQueueRepository.deleteById(competitivePlayers[1].getUserId());

                //Удаляем из очереди
                hangmanRegistry.removeFromCompetitiveQueue(competitivePlayers[0].getUserId());
                hangmanRegistry.removeFromCompetitiveQueue(competitivePlayers[1].getUserId());
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