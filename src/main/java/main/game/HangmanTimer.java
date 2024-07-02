package main.game;

import main.enums.GameStatus;
import main.game.core.HangmanRegistry;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

@Service
public class HangmanTimer extends TimerTask {

    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private final HangmanGameRepository hangmanGameRepository;
    private final Logger LOGGER = Logger.getLogger(HangmanTimer.class.getName());

    @Autowired
    public HangmanTimer(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
        Timer timer = new Timer();
        timer.schedule(this, 0, 5000);
    }

    @Override
    public void run() {
        Collection<Hangman> allGames = HangmanRegistry.getInstance().getAllGames();
        allGames.forEach(hangman -> {
                    Timestamp localTimestamp = new Timestamp(System.currentTimeMillis());
                    Timestamp hangmanTimestamp = HangmanRegistry.getInstance().getHangmanTimer(hangman);
                    if (hangmanTimestamp != null && localTimestamp.after(hangmanTimestamp)) {
                        hangman.setGameStatus(GameStatus.TIME_OVER);
                        try {
                            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                            HangmanPlayer hangmanPlayer = hangmanPlayers[0];
                            long userId = hangmanPlayer.getUserId();

                            if (HangmanRegistry.getInstance().hasHangman(userId)) {
                                String gameOver = jsonGameParsers.getLocale("gameOver", userId);
                                String timeIsOver = jsonGameParsers.getLocale("timeIsOver", userId);
                                String gamePlayer;
                                if (hangmanPlayers.length == 1) {
                                    gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
                                } else {
                                    gamePlayer = jsonGameParsers.getLocale("Game_Players", userId);
                                }

                                EmbedBuilder info = new EmbedBuilder();
                                info.setColor(Color.GREEN);
                                info.setTitle(gameOver);
                                info.setDescription(timeIsOver);
                                info.addField(gamePlayer, hangman.getUserIdWithDiscord(), false);

                                HangmanEmbedUtils.editMessageWithButtons(info, userId, hangmanGameRepository);
                                hangmanGameRepository.deleteActiveGame(userId);
                                HangmanRegistry.getInstance().removeHangman(userId);
                            }
                        } catch (Exception e) {
                            LOGGER.info(e.getMessage());
                        }
                    }
                }
        );
    }
}