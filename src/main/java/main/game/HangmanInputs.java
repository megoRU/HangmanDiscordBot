package main.game;

import lombok.AllArgsConstructor;
import main.enums.GameStatus;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HangmanInputs {

    private final static Logger LOGGER = LoggerFactory.getLogger(HangmanInputs.class.getName());
    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanGameEndHandler hangmanGameEndHandler;

    public void handler(@NotNull final String input, @NotNull final Message messages, Hangman hangman) {
        long userId = messages.getAuthor().getIdLong();
        MessageDeleting.addMessageToDelete(messages);
        handleInput(input, userId, hangman);
    }

    public void handler(@NotNull final String input, final long userId, Hangman hangman) {
        handleInput(input, userId, hangman);
    }

    private void handleInput(@NotNull final String input, final long userId, Hangman hangman) {
        try {
            String word = hangman.getWORD();
            if (word == null) throw new NullPointerException();

            if (hangman.isLetterPresent(input.toUpperCase())) {
                handleLetterPresentInput(userId, hangman);
            } else if (input.length() == 1 && hangman.getWORD_HIDDEN().contains("_")) {
                handleSingleLetterInput(input, userId, hangman);
            } else if (input.length() == word.length()) {
                handleWordInput(input, userId, hangman);
            } else {
                handleWordWrongLengthInput(userId, hangman);
            }
        } catch (NullPointerException e) {
            LOGGER.error("WORD is null", e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void handleLetterPresentInput(long userId, Hangman hangman) {
        if (hangman.getGameStatus() == GameStatus.SAME_LETTER) return;
        hangman.setGameStatus(GameStatus.SAME_LETTER);
        String gameYouUseThisLetter = jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId);
        EmbedBuilder info = HangmanEmbedUtils.hangmanLayout(userId, gameYouUseThisLetter);
        HangmanEmbedUtils.editMessage(info, userId, false, hangmanGameRepository);
    }

    private void handleWordWrongLengthInput(long userId, Hangman hangman) {
        hangman.setGameStatus(GameStatus.WRONG_WORD);
        String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userId);
        EmbedBuilder wrongLength = HangmanEmbedUtils.hangmanLayout(userId, wrongLengthJson);
        HangmanEmbedUtils.editMessage(wrongLength, userId, false, hangmanGameRepository);
    }

    private void handleSingleLetterInput(String input, long userId, Hangman hangman) {
        if (hangman.getWORD().contains(input)) {
            hangman.setGameStatus(GameStatus.RIGHT_LETTER);
            String result = hangman.replacementLetters(input);
            //Игрок угадал все буквы
            if (!result.contains("_")) {
                hangman.setGameStatus(GameStatus.WIN_GAME);
                hangmanGameEndHandler.handleGameEnd(hangman, true);
            } else {
                String gameYouGuessLetter = jsonGameParsers.getLocale("Game_You_Guess_Letter", userId);
                EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanLayout(userId, gameYouGuessLetter);
                HangmanEmbedUtils.editMessage(embedBuilder, userId, false, hangmanGameRepository);
            }
        } else {
            hangman.incrementHangmanErrors();

            if (hangman.getHangmanErrors() >= 8) {
                hangman.setGameStatus(GameStatus.LOSE_GAME);
                hangmanGameEndHandler.handleGameEnd(hangman, false);
            } else {
                hangman.setGameStatus(GameStatus.WRONG_LETTER);
                String gameNoSuchLetter = jsonGameParsers.getLocale("Game_No_Such_Letter", userId);
                EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanLayout(userId, gameNoSuchLetter);
                HangmanEmbedUtils.editMessage(wordNotFound, userId, false, hangmanGameRepository);
            }
        }
    }

    private void handleWordInput(String input, long userId, Hangman hangman) {
        if (input.equals(hangman.getWORD())) {
            hangman.setGameStatus(GameStatus.WIN_GAME);
            hangmanGameEndHandler.handleGameEnd(hangman, true);
        } else {
            hangman.incrementHangmanErrors();

            if (hangman.getHangmanErrors() >= 8) {
                hangman.setGameStatus(GameStatus.LOSE_GAME);
                hangmanGameEndHandler.handleGameEnd(hangman, false);
            } else {
                hangman.setGameStatus(GameStatus.WRONG_WORD);
                String gameNoSuchWord = jsonGameParsers.getLocale("Game_No_Such_Word", userId);
                EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanLayout(userId, gameNoSuchWord);
                HangmanEmbedUtils.editMessage(wordNotFound, userId, false, hangmanGameRepository);
            }
        }
    }
}