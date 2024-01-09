package main.service;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CompetitiveService {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final UpdateController updateController;
    private final HangmanResult hangmanResult;

    private final MegoruAPI megoruAPI = new MegoruAPI.Builder().build();

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
    }

    public void startGame() throws UnsuccessfulHttpException, IOException {
        HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();
        int competitiveQueueSize = hangmanRegistry.getCompetitiveQueueSize();
        if (competitiveQueueSize > 1) {
            HangmanPlayer[] competitivePlayers = hangmanRegistry.getCompetitivePlayers();
            if (competitivePlayers.length > 1) {
                UserSettings.GameLanguage gameLanguage = competitivePlayers[0].getGameLanguage();

                GameWordLanguage gameWordLanguage = new GameWordLanguage();
                gameWordLanguage.setLanguage(gameLanguage.name());
                gameWordLanguage.setCategory("ALL");

                String word = megoruAPI.getWord(gameWordLanguage).getWord();
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