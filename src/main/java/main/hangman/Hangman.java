package main.hangman;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPIImpl;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.hangman.impl.EndGameButtons;
import main.hangman.impl.GetImage;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

public class Hangman implements HangmanHelper {

    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    //Letters
    private static final String[] ALPHABET_EN = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] ALPHABET_RU = {"А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л",
            "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь", "Э", "Ю", "Я"};

    //Logger
    private final Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    //Repository
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    private final Set<String> guesses = new LinkedHashSet<>();

    //User|Guild|Channel data
    private final long userId;
    private final Long guildId;
    private Long channelId;
    private final String userIdWithDiscord;

    private int countUsedLetters;
    private String WORD;
    private String[] wordToChar;
    private String WORD_HIDDEN;
    private String currentHiddenWord;
    private int hangmanErrors;

    public Hangman(long userId, Long guildId, Long channelId,
                   HangmanGameRepository hangmanGameRepository,
                   GamesRepository gamesRepository,
                   PlayerRepository playerRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userIdWithDiscord = String.format("<@%s>", userId);
    }

    //TODO: Работает, но изменить время на Instant желательно.
    private EmbedBuilder updateEmbedBuilder() {
        autoInsert();

        if (BotStartConfig.getMapGameMode().get(userId).equals("select-menu")) {
            String gameStart = jsonGameParsers.getLocale("Game_Start", userId);
            return embedBuilder(
                    Color.GREEN,
                    gameStart,
                    false,
                    false,
                    null);
        } else {
            String gameStart2 = jsonGameParsers.getLocale("Game_Start2", userId);
            return embedBuilder(
                    Color.GREEN,
                    gameStart2,
                    false,
                    false,
                    null
            );
        }
    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        if (BotStartConfig.getMapGameLanguages().get(userId) == null
                || BotStartConfig.getMapGameMode().get(userId) == null) {
            EmbedBuilder needSetLanguage = new EmbedBuilder();

            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId);

            needSetLanguage.setAuthor(userName, null, avatarUrl);
            needSetLanguage.setColor(Color.GREEN);
            needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

            textChannel.sendMessageEmbeds(needSetLanguage.build())
                    .addActionRow(
                            Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
                            Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")))
                    .addActionRow(
                            Button.danger(Buttons.BUTTON_SELECT_MENU.name(), "Guild/DM: SelectMenu"),
                            Button.success(Buttons.BUTTON_DM.name(), "(Recommended) Only DM: One letter in chat"))
                    .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"))
                    .queue();

            HangmanRegistry.getInstance().removeHangman(userId);
            return;
        }

        MegoruAPI megoruAPI = new MegoruAPIImpl("this bot don`t use token");
        GameWordLanguage gameWordLanguage = new GameWordLanguage();
        gameWordLanguage.setLanguage(BotStartConfig.getMapGameLanguages().get(userId));

        try {
            WORD = megoruAPI.getWord(gameWordLanguage).getWord();
            if (WORD != null) {
                wordToChar = WORD.split(""); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            }
        } catch (Exception e) {
            String errorsTitle = jsonParsers.getLocale("errors_title", userId);
            String errors = jsonParsers.getLocale("errors", userId);

            EmbedBuilder wordIsNull = new EmbedBuilder();
            wordIsNull.setTitle(errorsTitle);
            wordIsNull.setAuthor(userName, null, avatarUrl);
            wordIsNull.setColor(Color.RED);
            wordIsNull.setDescription(errors);

            textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
            HangmanRegistry.getInstance().removeHangman(userId);
            return;
        }

        Message message;
        if (BotStartConfig.getMapGameMode().get(userId).equals("select-menu")) {
            List<SelectMenu> selectMenuList = selectMenus();
            message = textChannel.sendMessageEmbeds(updateEmbedBuilder().build())
                    .addActionRow(selectMenuList.get(0))
                    .addActionRow(selectMenuList.get(1))
                    .complete();

        } else {
            message = textChannel.sendMessageEmbeds(updateEmbedBuilder().build()).complete();
        }

        createEntityInDataBase(message);

        //Установка авто завершения
        setAutoCancel(LocalDateTime.now());
    }

    //TODO: Возможно произойдет так что игру закончили. Удалили данные из БД и произойдет REPLACE и игра не завершится
    private void executeInsert() {
        try {
            if ((guesses.size() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(userId)) {
                countUsedLetters = guesses.size();
                System.out.println("currentHiddenWord: " + currentHiddenWord);
                System.out.println("getGuesses(): " + getGuesses());
                hangmanGameRepository.updateGame(userId, currentHiddenWord, getGuesses(), hangmanErrors);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    //Автоматически отправляет в БД данные
    public void autoInsert() {
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (HangmanRegistry.getInstance().hasHangman(userId)) {
                        executeInsert();
                    } else {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.info(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }, 7000, 5000);
    }

    public void fullWord(String inputs) {
        try {
            if (inputs.length() < WORD.length() || inputs.length() > WORD.length()) {
                String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userId);
                EmbedBuilder wrongLength;
                if (guesses.isEmpty()) {
                    wrongLength = embedBuilder(
                            Color.GREEN,
                            wrongLengthJson,
                            false,
                            false,
                            inputs);
                } else {
                    wrongLength = embedBuilder(
                            Color.GREEN,
                            wrongLengthJson,
                            true,
                            false,
                            inputs);
                }

                HangmanHelper.editMessage(wrongLength, userId);
                return;
            }

            if (isLetterPresent(inputs.toUpperCase())) {
                String gameYouUseThisWord = jsonGameParsers.getLocale("Game_You_Use_This_Word", userId);

                EmbedBuilder info = embedBuilder(
                        Color.GREEN,
                        gameYouUseThisWord,
                        true,
                        false,
                        inputs
                );
                HangmanHelper.editMessage(info, userId);
                return;
            }

            if (inputs.equals(WORD)) {
                String gameStopWin = jsonGameParsers.getLocale("Game_Stop_Win", userId);

                EmbedBuilder win = embedBuilder(
                        Color.GREEN,
                        gameStopWin,
                        true,
                        false,
                        inputs
                );

                HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId));

                ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, true);
                resultGame.send();
                HangmanRegistry.getInstance().removeHangman(userId);
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    gameLose(inputs);
                } else {
                    String gameNoSuchWord = jsonGameParsers.getLocale("Game_No_Such_Word", userId);

                    EmbedBuilder wordNotFound = embedBuilder(
                            Color.GREEN,
                            gameNoSuchWord,
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(wordNotFound, userId);
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void gameLose(String inputs) {
        try {
            String gameYouLose = jsonGameParsers.getLocale("Game_You_Lose", userId);

            EmbedBuilder info = embedBuilder(
                    Color.GREEN,
                    gameYouLose,
                    true,
                    true,
                    inputs
            );

            HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId));

            ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, false);
            resultGame.send();
            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void logic(String inputs, Message messages) {
        try {
            if (WORD == null) {
                String wordIsNull = jsonParsers.getLocale("word_is_null", userId);

                messages.getChannel()
                        .sendMessage(wordIsNull)
                        .setActionRow(EndGameButtons.getListButtons(userId))
                        .queue();
                HangmanRegistry.getInstance().removeHangman(userId);
                return;
            }
        } catch (Exception e) {
            System.out.println("Word null");
        }
        try {
            if (WORD_HIDDEN.contains("_")) {
                if (isLetterPresent(inputs.toUpperCase())) {
                    String gameYouUseThisLetter = jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId);

                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            gameYouUseThisLetter,
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, userId);
                    return;
                }

                if (WORD.contains(inputs)) {
                    String result = replacementLetters(inputs);

                    //Игрок угадал все буквы
                    if (!result.contains("_")) {
                        String gameStopWin = jsonGameParsers.getLocale("Game_Stop_Win", userId);

                        EmbedBuilder win = embedBuilder(
                                Color.GREEN,
                                gameStopWin,
                                true,
                                false,
                                result
                        );

                        HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId));

                        ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, true);
                        resultGame.send();
                        HangmanRegistry.getInstance().removeHangman(userId);
                        return;
                    }
                    String gameYouGuessLetter = jsonGameParsers.getLocale("Game_You_Guess_Letter", userId);

                    //Вы угадали букву!
                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            gameYouGuessLetter,
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, userId);
                } else {
                    hangmanErrors++;
                    if (hangmanErrors >= 8) {
                        gameLose(inputs);
                    } else {
                        String gameNoSuchLetter = jsonGameParsers.getLocale("Game_No_Such_Letter", userId);

                        EmbedBuilder wordNotFound = embedBuilder(
                                Color.GREEN,
                                gameNoSuchLetter,
                                true,
                                false,
                                inputs
                        );

                        HangmanHelper.editMessage(wordNotFound, userId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public EmbedBuilder embedBuilder(Color color, String gameInfo, boolean gameGuesses, boolean isDefeat, @Nullable String inputs) {
        EmbedBuilder embedBuilder = null;
        try {
            String language = BotStartConfig.getMapGameLanguages().get(userId).equals("rus") ? "Кириллица" : "Latin";
            embedBuilder = new EmbedBuilder();

            LOGGER.info("\ngamePlayer: " + userIdWithDiscord
                    + "\ngameInfo: " + gameInfo
                    + "\ngameGuesses: " + gameGuesses
                    + "\nisDefeat: " + isDefeat
                    + "\ninputs: " + inputs
                    + "\nlanguage " + language);

            String gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
            String gameLanguage = jsonGameParsers.getLocale("Game_Language", userId);

            embedBuilder.setColor(color);
            embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
            embedBuilder.addField(gameLanguage, language, true);
            embedBuilder.setThumbnail(GetImage.get(hangmanErrors));

            if (gameGuesses) {
                String gameGuessesL = jsonGameParsers.getLocale("Game_Guesses", userId);
                embedBuilder.addField(gameGuessesL, "`" + getGuesses() + "`", false);
            }

            if (inputs != null && inputs.length() == 1) {
                String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
                embedBuilder.addField(gameCurrentWord, "`" + replacementLetters(inputs).toUpperCase() + "`", false);
            }

            if (inputs == null) {
                String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
                String worldUpper = String.format("`%s`", WORD_HIDDEN.toUpperCase());

                embedBuilder.addField(gameCurrentWord, worldUpper, false);
            } else if (inputs.length() >= 3) {
                String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
                if (inputs.equals(WORD)) {
                    String worldUpper = String.format("`%s`", WORD.toUpperCase().replaceAll("", " ").trim());
                    embedBuilder.addField(gameCurrentWord, worldUpper, false);
                } else {
                    String worldUpper = String.format("`%s`", WORD_HIDDEN.toUpperCase());
                    embedBuilder.addField(gameCurrentWord, worldUpper, false);
                }
            }

            if (isDefeat) {
                String gameWordThatWas = jsonGameParsers.getLocale("Game_Word_That_Was", userId);
                String worldUpper = String.format("`%s`", WORD.toUpperCase().replaceAll("", " ").trim() + "`");
                embedBuilder.addField(gameWordThatWas, worldUpper, false);
            }
            String gameInfo1 = jsonGameParsers.getLocale("Game_Info", userId);
            embedBuilder.addField(gameInfo1, gameInfo, false);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        //embedBuilder.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(userId))));
        //embedBuilder.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
        return embedBuilder;
    }

    private void createEntityInDataBase(Message message) {
        try {
            HangmanRegistry.getInstance().getMessageId().put(userId, message.getId());
            channelId = message.getChannel().getIdLong();
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toLocalDateTime().plusHours(3));

            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(userId);
            activeHangman.setMessageIdLong(message.getIdLong());
            activeHangman.setChannelIdLong(message.getChannel().getIdLong());
            activeHangman.setGuildLongId(guildId);
            activeHangman.setWord(WORD);
            activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
            activeHangman.setHangmanErrors(hangmanErrors);
            activeHangman.setGameCreatedTime(timestamp);
            hangmanGameRepository.saveAndFlush(activeHangman);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void stopGameByTime() {
        try {
            String gameOver = jsonGameParsers.getLocale("gameOver", userId);
            String timeIsOver = jsonGameParsers.getLocale("timeIsOver", userId);
            String gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);

            EmbedBuilder info = new EmbedBuilder();
            info.setColor(0x00FF00);
            info.setTitle(gameOver);
            info.setDescription(timeIsOver);
            info.addField(gamePlayer, userIdWithDiscord, false);

            HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId));
            HangmanRegistry.getInstance().getTimeCreatedGame().remove(userId);
            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            if (e.getMessage().contains("10008: Unknown Message")) {
                return;
            }
            LOGGER.info(e.getMessage());
        }
    }

    private void setAutoCancel(LocalDateTime ldt) {
        Timer timer = new Timer();
        StopHangmanTimer stopGiveawayByTimer = new StopHangmanTimer();
        ZonedDateTime localDateTime = ldt.atZone(ZoneId.systemDefault());
        HangmanRegistry.getInstance().getTimeCreatedGame().put(userId, localDateTime.toLocalDateTime());
        Date date = Date.from(localDateTime.plusMinutes(10).toInstant());
        timer.schedule(stopGiveawayByTimer, date);
    }

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        try {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < length) {
                sb.append(sb.length() == 0 ? "_" : " _");
                i++;
            }
            WORD_HIDDEN = sb.toString();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    //заменяет "_" на букву которая есть в слове
    private String replacementLetters(String letter) {
        try {
            if (currentHiddenWord == null) currentHiddenWord = WORD_HIDDEN;

            StringBuilder sb = new StringBuilder(currentHiddenWord);
            for (int i = 0; i < wordToChar.length; i++) {
                if (wordToChar[i].equals(letter)) {
                    sb.replace(
                            i == 0 ? i : i * 2,
                            i == 0 ? i + 1 : i * 2 + 1,
                            String.valueOf(wordToChar[i]));
                }
            }
            currentHiddenWord = sb.toString();
            WORD_HIDDEN = currentHiddenWord;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }
        return currentHiddenWord;
    }

    //Для инъекции при восстановлении
    public void updateVariables(String guesses, String word, String currentHiddenWord, int hangmanErrors, LocalDateTime localDateTime) {
        if (this.guesses.isEmpty() && this.WORD == null && this.currentHiddenWord == null && this.hangmanErrors == 0) {
            if (guesses != null) {
                this.guesses.addAll(Arrays.asList(guesses.split(", ")));
            }
            this.WORD = word;
            this.WORD_HIDDEN = currentHiddenWord;
            this.currentHiddenWord = currentHiddenWord;
            this.hangmanErrors = hangmanErrors;
            this.wordToChar = word.split("");
            setAutoCancel(localDateTime);
        } else {
            System.out.println("Вы не можете менять значения. Это нарушает инкапсуляцию!");
        }
    }

    private boolean isLetterPresent(String inputs) {
        boolean contains = guesses.contains(inputs.toUpperCase());
        if (!contains) {
            addGuesses(inputs.toUpperCase());
        }
        return contains;
    }

    private void addGuesses(String letter) {
        guesses.add(letter);
    }

    public String getGuesses() {
        return guesses
                .toString()
                .replaceAll("\\[", "")
                .replaceAll("]", "");
    }

    private List<SelectMenu> selectMenus() {
        if (BotStartConfig.getMapGameLanguages().get(userId).equals("eng")) {
            final int half = ALPHABET_EN.length / 2;

            SelectMenu.Builder builder = SelectMenu.create("menu:")
                    .setPlaceholder("A-M Letters")
                    .setRequiredRange(1, 1);

            SelectMenu.Builder builder2 = SelectMenu.create("menu:2")
                    .setPlaceholder("N-Z Letters")
                    .setRequiredRange(1, 1);

            for (int i = 0; i < ALPHABET_EN.length; i++) {
                if (i < half) {
                    builder.addOption(ALPHABET_EN[i], ALPHABET_EN[i].toLowerCase());
                } else {
                    builder2.addOption(ALPHABET_EN[i], ALPHABET_EN[i].toLowerCase());
                }
            }

            SelectMenu menu = builder.build();
            SelectMenu menu2 = builder2.build();

            List<SelectMenu> selectMenuList = new ArrayList<>(2);
            selectMenuList.add(menu);
            selectMenuList.add(menu2);
            return selectMenuList;
        } else {
            final int half = ALPHABET_RU.length / 2;

            SelectMenu.Builder builder = SelectMenu.create("menu:")
                    .setPlaceholder("А-О Буквы")
                    .setRequiredRange(1, 1);

            SelectMenu.Builder builder2 = SelectMenu.create("menu:2")
                    .setPlaceholder("П-Я Буквы")
                    .setRequiredRange(1, 1);

            for (int i = 0; i < ALPHABET_RU.length; i++) {
                if (i < half) {
                    builder.addOption(ALPHABET_RU[i], ALPHABET_RU[i].toLowerCase());
                } else {
                    builder2.addOption(ALPHABET_RU[i], ALPHABET_RU[i].toLowerCase());
                }
            }

            SelectMenu menu = builder.build();
            SelectMenu menu2 = builder2.build();

            List<SelectMenu> selectMenuList = new ArrayList<>(2);
            selectMenuList.add(menu);
            selectMenuList.add(menu2);
            return selectMenuList;
        }
    }

    public Long getGuildId() {
        return guildId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public final class StopHangmanTimer extends TimerTask {

        @Override
        public void run() {
            try {
                if (HangmanRegistry.getInstance().hasHangman(userId)) {
                    stopGameByTime();
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

    }
}