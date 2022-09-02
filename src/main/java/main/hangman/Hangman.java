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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

public class Hangman implements HangmanHelper {

    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    //Logger
    private final Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    //Repository
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    private final Set<String> guesses;

    public final List<Message> messageList;

    //User|Guild|Channel data
    private final long userId;
    private final Long guildId;
    private final Long channelId;
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
        this.guesses = new LinkedHashSet<>();
        this.currentHiddenWord = null;
        this.messageList = new LinkedList<>();
    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        if (!BotStartConfig.getMapGameLanguages().containsKey(userId)) {
            EmbedBuilder needSetLanguage = new EmbedBuilder();

            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId);

            needSetLanguage.setAuthor(userName, null, avatarUrl);
            needSetLanguage.setColor(Color.GREEN);
            needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

            textChannel.sendMessageEmbeds(needSetLanguage.build())
                    .addActionRow(
                            Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица").withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
                            Button.secondary(Buttons.BUTTON_ENG.name(), "Latin").withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")))
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

        String gameStart = jsonGameParsers.getLocale("Game_Start", userId);
        EmbedBuilder start = embedBuilder(
                Color.GREEN,
                gameStart,
                false,
                false,
                null
        );

        Message message = textChannel.sendMessageEmbeds(start.build()).complete();

        createEntityInDataBase(message);

        //Установка авто завершения
        setTimer(LocalDateTime.now());
        autoInsert();
        autoDeletingMessages();
    }

    public void fullWord(final String inputs) {
        try {
            if (inputs.length() != getLengthWord()) {
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

            if (isLetterPresent(inputs)) {
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
                gameWin(inputs);
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

    private void gameWin(String inputs) {
        try {
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

    public synchronized void logic(final String inputs, final Message messages) {
        try {
            messageList.add(messages);
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
            e.printStackTrace();
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
                        gameWin(inputs);
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
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }
    }

    public EmbedBuilder embedBuilder(Color color, String gameInfo, boolean gameGuesses, boolean isDefeat, @Nullable String inputs) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String language = BotStartConfig.getMapGameLanguages().get(userId).equals("rus") ? "Кириллица" : "Latin";

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
            String guesses = String.format("`%s`", getGuesses());

            embedBuilder.addField(gameGuessesL, guesses, false);
        }

        if (inputs != null && inputs.length() == 1) {
            String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
            String worldUpper = String.format("`%s`", replacementLetters(inputs).toUpperCase());

            embedBuilder.addField(gameCurrentWord, worldUpper, false);
        }

        if (inputs == null) {
            String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
            String worldUpper = String.format("`%s`", WORD_HIDDEN.toUpperCase());

            embedBuilder.addField(gameCurrentWord, worldUpper, false);
        } else if (inputs.length() >= 3) {
            String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
            String worldUpper;
            if (inputs.equals(WORD)) {
                worldUpper = String.format("`%s`", WORD.toUpperCase().replaceAll("", " ").trim());
            } else {
                worldUpper = String.format("`%s`", WORD_HIDDEN.toUpperCase());
            }
            embedBuilder.addField(gameCurrentWord, worldUpper, false);
        }

        if (isDefeat) {
            String gameWordThatWas = jsonGameParsers.getLocale("Game_Word_That_Was", userId);
            String worldUpper = String.format("`%s`", WORD.toUpperCase().replaceAll("", " ").trim());
            embedBuilder.addField(gameWordThatWas, worldUpper, false);
        }
        String gameInfo1 = jsonGameParsers.getLocale("Game_Info", userId);
        embedBuilder.addField(gameInfo1, gameInfo, false);
        return embedBuilder;
    }

    private void createEntityInDataBase(Message message) {
        try {
            HangmanRegistry.getInstance().setMessageId(userId, message.getId());
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());

            System.out.println(timestamp);

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
            if (HangmanRegistry.getInstance().hasHangman(this.userId)) {
                String gameOver = jsonGameParsers.getLocale("gameOver", userId);
                String timeIsOver = jsonGameParsers.getLocale("timeIsOver", userId);
                String gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);

                EmbedBuilder info = new EmbedBuilder();
                info.setColor(0x00FF00);
                info.setTitle(gameOver);
                info.setDescription(timeIsOver);
                info.addField(gamePlayer, userIdWithDiscord, false);


                HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId));

                hangmanGameRepository.deleteActiveGame(userId);
                HangmanRegistry.getInstance().removeHangman(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTimer(LocalDateTime ldt) {
        Timer timer = new Timer();
        StopHangmanTimer stopGiveawayByTimer = new StopHangmanTimer();
        Timestamp timestamp = Timestamp.valueOf(ldt.atZone(ZoneOffset.UTC).toLocalDateTime().plusMinutes(10));
        Date date = new Date(timestamp.getTime());
        timer.schedule(stopGiveawayByTimer, date);
        HangmanRegistry.getInstance().setHangmanTimer(userId, timer);
    }

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            sb.append(sb.length() == 0 ? "_" : " _");
            i++;
        }
        WORD_HIDDEN = sb.toString();
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
            setTimer(localDateTime);
            autoInsert();
            autoDeletingMessages();
        } else {
            System.out.println("Вы не можете менять значения. Это нарушает инкапсуляцию!");
        }
    }

    private boolean isLetterPresent(final String inputs) {
        boolean contains = guesses.contains(inputs.toUpperCase());
        if (!contains) {
            guesses.add(inputs.toUpperCase());
        }
        return contains;
    }

    public String getGuesses() {
        return guesses
                .toString()
                .replaceAll("\\[", "")
                .replaceAll("]", "");
    }

    public Long getGuildId() {
        return guildId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public int getLengthWord() {
        return WORD != null ? WORD.length() : 0;
    }

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
    private void autoInsert() {
        AutoUpdate autoUpdate = new AutoUpdate();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(autoUpdate, 7000, 5000);
        HangmanRegistry.getInstance().setTimeAutoUpdate(userId, timer);
    }

    //Автоматически отправляет в БД данные
    private void autoDeletingMessages() {
        AutoDeletingMessages autoDeletingMessages = new AutoDeletingMessages();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(autoDeletingMessages, 7000, 5000);
        HangmanRegistry.getInstance().setAutoDeletingMessages(userId, timer);
    }

    private final class AutoDeletingMessages extends TimerTask {

        @Override
        public void run() {
            try {
                if (guildId == null) {
                    HangmanRegistry.getInstance().getAutoDeletingMessages(userId).cancel();
                    return;
                }

                Guild guildById = BotStartConfig.jda.getGuildById(guildId);

                if (guildById != null) {
                    Member selfMember = guildById.getSelfMember();
                    TextChannel textChannelById = BotStartConfig.jda.getTextChannelById(channelId);

                    if (textChannelById != null) {
                        if (selfMember.hasPermission(textChannelById, Permission.MESSAGE_MANAGE) && !messageList.isEmpty()) {
                            if (messageList.size() > 2) {
                                LOGGER.info("messageList.size(): " + messageList.size()
                                        + "\nmessageList: " + Arrays.toString(messageList.toArray()));
                                textChannelById.deleteMessages(messageList).submit().get();
                                //Так как метод асинхронный иногда может возникать NPE
                                Thread.sleep(2000);
                                messageList.clear();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final class AutoUpdate extends TimerTask {

        @Override
        public void run() {
            try {
                if (HangmanRegistry.getInstance().hasHangman(userId)) {
                    executeInsert();
                } else {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

    }

    private final class StopHangmanTimer extends TimerTask {

        @Override
        public void run() {
            try {
                if (HangmanRegistry.getInstance().hasHangman(userId)) {
                    stopGameByTime();
                } else {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

    }
}