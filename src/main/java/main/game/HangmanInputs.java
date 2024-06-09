package main.game;

import main.enums.GameStatus;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class HangmanInputs {

    private final Logger LOGGER = Logger.getLogger(HangmanInputs.class.getName());
    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private final HangmanGameRepository hangmanGameRepository;

    @Autowired
    public HangmanInputs(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
    }

    public void handler(@NotNull final String input, @NotNull final Message messages, Hangman hangman) {
        long userId = messages.getAuthor().getIdLong();
        try {
            MessageDeleting.addMessageToDelete(messages);
            if (hangman.getWORD() == null) throw new NullPointerException();

            if (hangman.isLetterPresent(input.toUpperCase())) {
                handleLetterPresentInput(userId, hangman);
            } else if (input.length() == 1 && hangman.getWORD_HIDDEN().contains("_")) {
                handleSingleLetterInput(input, userId, hangman);
            } else if (input.length() == hangman.getLengthWord()) {
                handleWordInput(input, userId, hangman);
            } else if (input.length() != hangman.getLengthWord()) {
                handleWordWrongLengthInput(userId, hangman);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void handleLetterPresentInput(long userId, Hangman hangman) {
        if (hangman.getGameStatus() == GameStatus.SAME_LETTER) return;
        hangman.setGameStatus(GameStatus.SAME_LETTER);
        String gameYouUseThisLetter = jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId);
        EmbedBuilder info = HangmanEmbedUtils.hangmanLayout(userId, gameYouUseThisLetter);
        HangmanEmbedUtils.editMessage(info, userId, hangmanGameRepository);
    }

    private void handleWordWrongLengthInput(long userId, Hangman hangman) {
        hangman.setGameStatus(GameStatus.WRONG_WORD);
        String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userId);
        EmbedBuilder wrongLength = HangmanEmbedUtils.hangmanLayout(userId, wrongLengthJson);
        HangmanEmbedUtils.editMessage(wrongLength, userId, hangmanGameRepository);
    }

    private void handleSingleLetterInput(String input, long userId, Hangman hangman) {
        if (hangman.getWORD().contains(input)) {
            hangman.setGameStatus(GameStatus.RIGHT_LETTER);
            String result = hangman.replacementLetters(input);
            //Игрок угадал все буквы
            if (!result.contains("_")) {
                hangman.setGameStatus(GameStatus.WIN_GAME);
                hangman.gameEnd(true);
            } else {
                String gameYouGuessLetter = jsonGameParsers.getLocale("Game_You_Guess_Letter", userId);
                EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, gameYouGuessLetter);
                HangmanEmbedUtils.editMessage(embedBuilder, userId, hangmanGameRepository);
            }
        } else {
            hangman.incrementHangmanErrors();

            if (hangman.getHangmanErrors() >= 8) {
                hangman.setGameStatus(GameStatus.LOSE_GAME);
                hangman.gameEnd(false);
            } else {
                hangman.setGameStatus(GameStatus.WRONG_LETTER);
                String gameNoSuchLetter = jsonGameParsers.getLocale("Game_No_Such_Letter", userId);
                EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanLayout(userId, gameNoSuchLetter);
                HangmanEmbedUtils.editMessage(wordNotFound, userId, hangmanGameRepository);
            }
        }
    }

    private void handleWordInput(String input, long userId, Hangman hangman) {
        if (input.equals(hangman.getWORD())) {
            hangman.setGameStatus(GameStatus.WIN_GAME);
            hangman.gameEnd(true);
        } else {
            hangman.incrementHangmanErrors();

            if (hangman.getHangmanErrors() >= 8) {
                hangman.setGameStatus(GameStatus.LOSE_GAME);
                hangman.gameEnd(false);
            } else {
                hangman.setGameStatus(GameStatus.WRONG_WORD);
                String gameNoSuchWord = jsonGameParsers.getLocale("Game_No_Such_Word", userId);
                EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanLayout(userId, gameNoSuchWord);
                HangmanEmbedUtils.editMessage(wordNotFound, userId, hangmanGameRepository);
            }
        }
    }
}