package main.game.utils;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HangmanUtils {

    //Localisation
    private static final JSONParsers JSON_BOT_PARSERS = new JSONParsers(JSONParsers.Locale.BOT);

    public static Button BUTTON_RUSSIAN = Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA"));
    public static Button BUTTON_ENGLISH = Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7"));
    public static Button BUTTON_MY_STATS = Button.primary(Buttons.BUTTON_MY_STATS.name(), "My stats");
    public static Button BUTTON_PLAY_AGAIN = Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again");
    public static Button BUTTON_SUPPORT = Button.link("https://discord.gg/UrWG3R683d", "Support");
    public static Button BUTTON_HELP = Button.success(Buttons.BUTTON_HELP.name(), "/help");
    public static Button BUTTON_STOP = Button.danger(Buttons.BUTTON_STOP.name(), "Stop game");

    public static Button getButtonPlayAgainWithUsers(long userIdLong, long secondUser) {
        String multi = String.format("%s_%s_%s", Buttons.BUTTON_START_NEW_GAME.name(), userIdLong, secondUser);
        return Button.success(multi, "Play again");
    }

    public static String getImage(int count) {
        return String.format("https://megoru.ru/hangman/%s.png", count);
    }

    public static List<Button> getListButtons(long userIdLong) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(BUTTON_PLAY_AGAIN);
        return getButtons(userIdLong, buttonList);
    }

    public static List<Button> getListButtons(long userIdLong, long secondUser) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(getButtonPlayAgainWithUsers(userIdLong, secondUser));
        return getButtons(userIdLong, buttonList);
    }

    @NotNull
    private static List<Button> getButtons(long userIdLong, List<Button> buttonList) {
        UserSettings.GameLanguage language = BotStartConfig.getMapGameLanguages().get(userIdLong);
        if (language != null && language.name().equals("EN")) {
            buttonList.add(BUTTON_RUSSIAN);
        } else {
            buttonList.add(BUTTON_ENGLISH);
        }
        buttonList.add(BUTTON_MY_STATS);
        return buttonList;
    }

    public static String getGuesses(Set<String> guesses) {
        return guesses
                .toString()
                .replaceAll("\\[", "")
                .replaceAll("]", "");
    }

    public static String category(Long userId) {
        UserSettings.Category category = BotStartConfig.getMapGameCategory().get(userId);
        UserSettings.BotLanguage language = BotStartConfig.getMapLanguages().get(userId);
        return switch (category.name()) {
            case "colors" -> Objects.equals(language.name(), "EN") ? "`Colors`" : "`Цвета`";
            case "flowers" -> Objects.equals(language.name(), "EN") ? "`Flowers`" : "`Цветы`";
            case "fruits" -> Objects.equals(language.name(), "EN") ? "`Fruits`" : "`Фрукты`";
            default -> Objects.equals(language.name(), "EN") ? "`Any`" : "`Любая`";
        };
    }

    public static long getHangmanFirstPlayer(HangmanPlayer[] hangmanPlayers) {
        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        return hangmanPlayer.getUserId();
    }

    public static void handleAPIException(long userId, MessageChannel textChannel) {
        String errorsTitle = JSON_BOT_PARSERS.getLocale("errors_title", userId);
        String errors = JSON_BOT_PARSERS.getLocale("errors", userId);

        EmbedBuilder wordIsNull = new EmbedBuilder();
        wordIsNull.setTitle(errorsTitle);
        wordIsNull.setColor(Color.RED);
        wordIsNull.setDescription(errors);

        textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
        HangmanRegistry.getInstance().removeHangman(userId);
    }
}