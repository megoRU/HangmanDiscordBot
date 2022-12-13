package main.hangman;

import main.config.BotStartConfig;
import main.hangman.impl.ImageURL;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

public class HangmanEmbedUtils {

    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    public static EmbedBuilder hangmanPattern(Long userId, String status) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);

        if (hangman != null) {
            String userIdWithDiscord = hangman.getUserIdWithDiscord();
            long secondPlayer = hangman.getSecondPlayer();
            int hangmanErrors = hangman.getHangmanErrors();
            String wordHidden = hangman.getWORD_HIDDEN();
            String guesses = hangman.getGuesses();

            String gamePlayer;
            if (secondPlayer == 0L) {
                gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
            } else {
                gamePlayer = jsonGameParsers.getLocale("Game_Players", userId);
            }

            Map<Long, String> mapGameLanguages = BotStartConfig.getMapGameLanguages();
            String gameLanguage = jsonGameParsers.getLocale("Game_Language", userId);
            String language = mapGameLanguages.get(userId).equals("rus") ? "Кириллица\nКатег.: " + category(userId) : "Latin\nCateg.:" + category(userId);

            embedBuilder.setColor(Color.GREEN);
            //Gamers
            embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
            //Game Language
            embedBuilder.addField(gameLanguage, language, true);
            //Image
            embedBuilder.setThumbnail(ImageURL.get(hangmanErrors));

            //Guesses
            if (guesses.length() > 0) {
                String gameGuesses = jsonGameParsers.getLocale("Game_Guesses", userId);
                String guessesFormat = String.format("`%s`", guesses);
                embedBuilder.addField(gameGuesses, guessesFormat, false);
            }

            //Current Hidden Word
            String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
            String worldUpper = String.format("`%s`", wordHidden);
            embedBuilder.addField(gameCurrentWord, worldUpper, false);

            //Status
            String gameInfo = jsonGameParsers.getLocale("Game_Info", userId);
            embedBuilder.addField(gameInfo, status, false);
        }

        return embedBuilder;
    }

















    private static String category(Long userId) {
        String category = BotStartConfig.getMapGameCategory().get(userId);
        String language = BotStartConfig.getMapLanguages().get(userId);
        if (category == null) return Objects.equals(language, "eng") ? "`Any`" : "`Любая`";
        return switch (category) {
            case "colors" -> Objects.equals(language, "eng") ? "`Colors`" : "`Цвета`";
            case "flowers" -> Objects.equals(language, "eng") ? "`Flowers`" : "`Цветы`";
            case "fruits" -> Objects.equals(language, "eng") ? "`Fruits`" : "`Фрукты`";
            default -> Objects.equals(language, "eng") ? "`Any`" : "`Любая`";
        };
    }

}
