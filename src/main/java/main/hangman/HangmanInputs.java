package main.hangman;

import main.controller.UpdateController;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.junit.internal.Checks;

public class HangmanInputs {

    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final Hangman hangman;
    private final UpdateController updateController;

    public HangmanInputs(Hangman hangman, UpdateController updateController1) {
        this.hangman = hangman;
        this.updateController = updateController1;
    }

    public void handler(@NotNull final String inputs, @NotNull final Message messages) {
        long userId = messages.getAuthor().getIdLong();
        try {
            MessageDeleting.addMessageToDelete(messages);
            Checks.notNull(hangman.getWORD());
            if (inputs.length() == 1) {
                if (hangman.getWORD_HIDDEN().contains("_")) {
                    if (hangman.isLetterPresent(inputs.toUpperCase())) {
                        if (hangman.getSTATUS() == Hangman.Status.SAME_LETTER) return;
                        hangman.setSTATUS(Hangman.Status.SAME_LETTER);
                        String gameYouUseThisLetter = jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId);
                        EmbedBuilder info = HangmanEmbedUtils.hangmanPattern(userId, gameYouUseThisLetter);
                        HangmanEmbedUtils.editMessage(info, userId, updateController.getHangmanGameRepository());
                        return;
                    }

                    if (hangman.getWORD().contains(inputs)) {
                        hangman.setSTATUS(Hangman.Status.RIGHT_LETTER);
                        String result = hangman.replacementLetters(inputs);
                        //Игрок угадал все буквы
                        if (!result.contains("_")) {
                            hangman.setSTATUS(Hangman.Status.WIN_GAME);
                            hangman.gameEnd(true);
                            return;
                        }
                        String gameYouGuessLetter = jsonGameParsers.getLocale("Game_You_Guess_Letter", userId);
                        EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userId, gameYouGuessLetter);
                        HangmanEmbedUtils.editMessage(embedBuilder, userId, updateController.getHangmanGameRepository());
                    } else {
                        hangman.incrementHangmanErrors();
                        if (hangman.getHangmanErrors() >= 8) {
                            hangman.setSTATUS(Hangman.Status.LOSE_GAME);
                            hangman.gameEnd(false);
                        } else {
                            hangman.setSTATUS(Hangman.Status.WRONG_LETTER);
                            String gameNoSuchLetter = jsonGameParsers.getLocale("Game_No_Such_Letter", userId);
                            EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanPattern(userId, gameNoSuchLetter);
                            HangmanEmbedUtils.editMessage(wordNotFound, userId, updateController.getHangmanGameRepository());
                        }
                    }
                }
            } else {
                if (inputs.length() != hangman.getLengthWord()) {
                    String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userId);
                    EmbedBuilder wrongLength = HangmanEmbedUtils.hangmanPattern(userId, wrongLengthJson);
                    HangmanEmbedUtils.editMessage(wrongLength, userId, updateController.getHangmanGameRepository());
                    return;
                }

                if (hangman.isLetterPresent(inputs)) {
                    if (hangman.getSTATUS() == Hangman.Status.SAME_LETTER) return;
                    hangman.setSTATUS(Hangman.Status.SAME_LETTER);
                    String gameYouUseThisWord = jsonGameParsers.getLocale("Game_You_Use_This_Word", userId);
                    EmbedBuilder info = HangmanEmbedUtils.hangmanPattern(userId, gameYouUseThisWord);
                    HangmanEmbedUtils.editMessage(info, userId, updateController.getHangmanGameRepository());
                    return;
                }

                if (inputs.equals(hangman.getWORD())) {
                    hangman.setSTATUS(Hangman.Status.WIN_GAME);
                    hangman.gameEnd(true);
                } else {
                    hangman.incrementHangmanErrors();
                    if (hangman.getHangmanErrors() >= 8) {
                        hangman.setSTATUS(Hangman.Status.LOSE_GAME);
                        hangman.gameEnd(false);
                    } else {
                        hangman.setSTATUS(Hangman.Status.WRONG_WORD);
                        String gameNoSuchWord = jsonGameParsers.getLocale("Game_No_Such_Word", userId);
                        EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanPattern(userId, gameNoSuchWord);
                        HangmanEmbedUtils.editMessage(wordNotFound, userId, updateController.getHangmanGameRepository());
                    }
                }
            }
        } catch (NullPointerException e) {
            String wordIsNull = jsonParsers.getLocale("word_is_null", userId);
            messages.getChannel()
                    .sendMessage(wordIsNull)
                    .setActionRow(HangmanUtils.getListButtons(userId))
                    .queue();

            HangmanRegistry.getInstance().removeHangman(userId);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
