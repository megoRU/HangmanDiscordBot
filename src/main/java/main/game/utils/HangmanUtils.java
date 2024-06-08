package main.game.utils;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.Hangman;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.service.UpdateStatisticsService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;

public class HangmanUtils {

    //Localisation
    private static final JSONParsers JSON_BOT_PARSERS = new JSONParsers(JSONParsers.Locale.BOT);

    public static Button BUTTON_RUSSIAN = Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA"));
    public static Button BUTTON_ENGLISH = Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7"));
    public static Button BUTTON_HELP = Button.success(Buttons.BUTTON_HELP.name(), "/help");

    public static Button getButtonLeaveSearch(long userId) {
        String buttonLeave = JSON_BOT_PARSERS.getLocale("button_leave", userId);
        return Button.danger(Buttons.BUTTON_COMPETITIVE_STOP.name(), buttonLeave);
    }

    public static Button getButtonStop(long userId) {
        String buttonStop = JSON_BOT_PARSERS.getLocale("button_stop", userId);
        return Button.danger(Buttons.BUTTON_STOP.name(), buttonStop);
    }

    public static Button getButtonPlayAgain(long userId) {
        String buttonPlay = JSON_BOT_PARSERS.getLocale("button_play", userId);
        return Button.success(Buttons.BUTTON_START_NEW_GAME.name(), buttonPlay);
    }

    public static Button getButtonSupport(long userId) {
        String buttonSupport = JSON_BOT_PARSERS.getLocale("button_support", userId);
        return Button.link("https://discord.gg/UrWG3R683d", buttonSupport);
    }

    public static Button getButtonPlayCompetitiveAgain(long userId) {
        String buttonPlayCompetitive = JSON_BOT_PARSERS.getLocale("button_play_competitive", userId);
        return Button.success(Buttons.BUTTON_COMPETITIVE_AGAIN.name(), buttonPlayCompetitive);
    }

    public static Button getButtonStatistics(long userId) {
        String buttonStatistics = JSON_BOT_PARSERS.getLocale("button_statistics", userId);
        return Button.primary(Buttons.BUTTON_MY_STATS.name(), buttonStatistics);
    }

    public static Button getButtonPlayAgainWithUsers(long userId) {
        String buttonPlayAgain = JSON_BOT_PARSERS.getLocale("button_play_again", userId);
        return Button.success(Buttons.BUTTON_START_NEW_GAME.name(), buttonPlayAgain);
    }

    public static String getImage(int count) {
        return String.format("https://megoru.ru/hangman/%s.png", count);
    }

//    public static List<Button> getListButtons(long userId) {
//        List<Button> buttonList = new LinkedList<>();
//        buttonList.add(getButtonPlayAgain(userId));
//        return getButtons(userId, buttonList);
//    }

    public static List<Long> getListUsersFromHangmanPlayers(HangmanPlayer[] hangmanPlayers) {
        return Arrays.stream(hangmanPlayers)
                .toList()
                .stream()
                .map(HangmanPlayer::getUserId)
                .toList();
    }

    public static List<Button> getListCompetitiveButtons(long userId) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(getButtonPlayCompetitiveAgain(userId));
        return buttonList;
    }

    public static List<Button> getListButtons(long userId) {
        List<Button> buttonList = new LinkedList<>();
        buttonList.add(getButtonPlayAgainWithUsers(userId));
        return getButtons(userId, buttonList);
    }

    @NotNull
    private static List<Button> getButtons(long userId, List<Button> buttonList) {
        UserSettings.GameLanguage language = BotStartConfig.getMapGameLanguages().get(userId);
        if (language != null && language.name().equals("EN")) {
            buttonList.add(BUTTON_RUSSIAN);
        } else {
            buttonList.add(BUTTON_ENGLISH);
        }
        buttonList.add(getButtonStatistics(userId));
        return buttonList;
    }

    public static void updateActivity(JDA jda) {
        update(jda, null);
    }

    public static void updateActivity(JDA jda, String string) {
        update(jda, string);
    }

    private static void update(JDA jda, String string) {
        if (jda != null) {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            int competitiveQueueSize = instance.getCompetitiveQueueSize();

            if (string != null) {
                jda.getPresence().setActivity(Activity.customStatus(
                        String.format("/competitive : [%s] | %s",
                                competitiveQueueSize,
                                string)));
            } else {
                String activity = UpdateStatisticsService.activity;
                jda.getPresence().setActivity(Activity.playing(activity));
            }
        }
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

        if (category == null) category = UserSettings.Category.ALL;
        if (language == null) language = UserSettings.BotLanguage.EN;

        return switch (category.name().toLowerCase()) {
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