package main.game;

import main.enums.GameStatus;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class HangmanGameEndHandler {

    private static final Logger LOGGER = Logger.getLogger(HangmanGameEndHandler.class.getName());
    //Localisation
    private static final JSONParsers JSON_GAME_PARSERS = new JSONParsers(JSONParsers.Locale.GAME);

    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanResult hangmanResult;

    @Autowired
    public HangmanGameEndHandler(HangmanGameRepository hangmanGameRepository, HangmanResult hangmanResult) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.hangmanResult = hangmanResult;
    }

    public void handleGameEnd(Hangman hangman, boolean result) {
        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            long userId = HangmanUtils.getHangmanFirstPlayer(hangman.getHangmanPlayers());
            boolean isCompetitive = hangman.isCompetitive();
            Long againstPlayerId = hangman.getAgainstPlayerId();

            //Это вроде чтобы двойное поражение не засчиталось
            if (!result && againstPlayerId != null) {
                Hangman activeHangman = instance.getActiveHangman(againstPlayerId);
                if (activeHangman != null) {
                    activeHangman.deleteAgainstPlayer();
                    hangmanGameRepository.updateGameOpponent(againstPlayerId);
                }
            }

            String gameStopWin = JSON_GAME_PARSERS.getLocale("Game_Stop_Win", userId);
            String gameYouLose = JSON_GAME_PARSERS.getLocale("Game_You_Lose", userId);
            String gameCompetitiveYouLose = JSON_GAME_PARSERS.getLocale("Game_Competitive_You_Lose", userId);

            //Чтобы было показано слово которое было
            if (result && isCompetitive && againstPlayerId != null) {
                instance.setHangmanStatus(againstPlayerId, GameStatus.LOSE_GAME);
            }

            EmbedBuilder win = HangmanEmbedUtils.hangmanLayout(userId, gameStopWin);
            EmbedBuilder lose = HangmanEmbedUtils.hangmanLayout(userId, gameYouLose);

            if (hangman.getHangmanPlayers().length == 1) {
                HangmanEmbedUtils.editMessageWithButtons(result ? win : lose, userId, hangmanGameRepository);
                if (hangman.isCompetitive() && result && againstPlayerId != null) {
                    EmbedBuilder competitiveLose = HangmanEmbedUtils.hangmanLayout(againstPlayerId, gameCompetitiveYouLose);
                    HangmanEmbedUtils.editMessageWithButtons(competitiveLose, againstPlayerId, hangmanGameRepository);
                }
            } else {
                HangmanEmbedUtils.editMessageWithButtons(result ? win : lose, userId, hangmanGameRepository);
            }

            //Люблю кастыли
            if (hangman.isCompetitive()) {
                long hangmanFirstPlayer = HangmanUtils.getHangmanFirstPlayer(hangman.getHangmanPlayers());
                hangmanResult.saveGame(hangmanFirstPlayer, result, true);
                if (result && againstPlayerId != null) {
                    if (!hangman.isOpponentLose()) { //чтобы не было двойного поражения
                        hangmanResult.saveGame(againstPlayerId, false, true);
                    }
                }
            } else {
                hangmanResult.saveGame(hangman.getHangmanPlayers(), result, false);
            }
            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in handleGameEnd", e);
        }
    }
}