package main.game;

import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.repository.HangmanGameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class HangmanUpdater extends TimerTask {

    private final HangmanGameRepository hangmanGameRepository;
    private final static Logger LOGGER = LoggerFactory.getLogger(HangmanUpdater.class.getName());

    @Autowired
    public HangmanUpdater(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
        Timer timer = new Timer();
        timer.schedule(this, 0, 5000);
    }

    @Override
    public void run() {
        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            Collection<Hangman> allGames = instance.getAllGames();
            allGames.forEach(hangman -> {
                        if (hangman.getGuessesSize() > hangman.getUsedLettersCount()) {
                            hangman.setUsedLettersCount(hangman.getGuessesSize());

                            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                            long userId = hangmanPlayers[0].getUserId();
                            String wordHidden = hangman.getWORD_HIDDEN();
                            String guesses = HangmanUtils.getGuesses(hangman.getGuesses());
                            int hangmanErrors = hangman.getHangmanErrors();

                            hangmanGameRepository.updateGame(userId, wordHidden, guesses, hangmanErrors);
                        }
                    }
            );
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}
