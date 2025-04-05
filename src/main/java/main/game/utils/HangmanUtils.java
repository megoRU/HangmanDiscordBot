package main.game.utils;

import main.config.BotStartConfig;
import main.enums.Buttons;
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
import java.util.stream.Collectors;

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
        return String.format("https://api.megoru.ru/hangman/%s.png", count);
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

    public static Button getButtonGPT(long userId) {
        String buttonPlayGpt = JSON_BOT_PARSERS.getLocale("button_play_gpt", userId);
        return Button.success(Buttons.BUTTON_START_GAME_GPT.name(), buttonPlayGpt);
    }

    @NotNull
    private static List<Button> getButtons(long userId, List<Button> buttonList) {
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userId);

        UserSettings.GameLanguage language = userSettings.getGameLanguage();
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

    public static boolean isChatGPT(long userId) {
        return String.valueOf(userId).contains("-");
    }

    public static String getGPTPrompt(@NotNull UserSettings.GameLanguage gameLanguage,
                                      UserSettings.Category gameCategory,
                                      String usedLetters,
                                      String hiddenWord) {
        List<String> lettersList = Arrays.stream(usedLetters.split(", ")).toList();

        if (gameLanguage == UserSettings.GameLanguage.RU) {
            List<Character> cyrillicLetters = new ArrayList<>(Arrays.asList(
                    'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И',
                    'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т',
                    'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь',
                    'Э', 'Ю', 'Я'
            ));

            if (!usedLetters.isEmpty()) {
                List<Character> usedCharacters = lettersList.stream()
                        .map(s -> s.charAt(0))
                        .toList();
                cyrillicLetters.removeAll(usedCharacters);
            }
            if (usedLetters.isEmpty()) usedLetters = "Ты ещё не использовал никакие буквы";

            return String.format("""
                            Отвечай одной буквой без дополнительного текста.
                            Игра Виселица Тебе нужно хорошо угадывать буквы в слове.
                            Черточки это скрытые буквы которые ты ещё не отгадал.
                            
                            Текущее слово: %s
                            Категория: %s
                            Слово состоит из %s букв
                            Использованные буквы: %s
                            Неиспользованные буквы: %s
                            """,
                    hiddenWord,
                    gameCategory.name(),
                    hiddenWord.length(),
                    usedLetters,
                    cyrillicLetters.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        } else {
            List<Character> latinLetters = new ArrayList<>(Arrays.asList(
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                    'U', 'V', 'W', 'X', 'Y', 'Z'
            ));

            if (!usedLetters.isEmpty()) {
                List<Character> usedCharacters = lettersList.stream()
                        .map(s -> s.charAt(0))
                        .toList();
                latinLetters.removeAll(usedCharacters);
            }
            if (usedLetters.isEmpty()) usedLetters = "You haven't used any letters yet";

            return String.format("""
                            Answer with one letter without additional text.
                            The Gallows game You need to guess the letters in the word well.
                            Dashes are hidden letters that you haven't guessed yet.
                            
                            Current word: %s
                            Category: %s
                            The word consists of %s letters
                            Letters used: %s
                            Unused letters: %s
                            """,
                    hiddenWord,
                    gameCategory.name(),
                    hiddenWord.length(),
                    usedLetters,
                    latinLetters.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        }
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
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userId);

        UserSettings.Category category = userSettings.getCategory();
        UserSettings.GameLanguage language = userSettings.getGameLanguage();

        if (category == null) category = UserSettings.Category.ALL;
        if (language == null) language = UserSettings.GameLanguage.EN;

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