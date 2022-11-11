package main.hangman;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPI;
import main.config.BotStartConfig;
import main.hangman.impl.ButtonIMpl;
import main.hangman.impl.EndGameButtons;
import main.hangman.impl.HangmanHelper;
import main.hangman.impl.ImageURL;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

    //Timers
    private final Timer autoDeletingTimer = new Timer();
    private final Timer autoInsert = new Timer();
    private final Timer stopHangmanTimer = new Timer();

    //Logger
    private final Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    //Repository
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    //API
    private final MegoruAPI megoruAPI = new MegoruAPI.Builder().build();

    private final Set<String> guesses;

    public final List<Message> messageList;

    //User|Guild|Channel data
    private final long userId;
    private final long secondPlayer;
    private final Long guildId;
    private final Long channelId;
    private final String userIdWithDiscord;

    private int countUsedLetters;
    private String WORD;
    private String[] WORD_OF_CHARS;
    private String WORD_HIDDEN;
    private int hangmanErrors;

    private volatile Status STATUS;

    Hangman(long userId, Long guildId, Long channelId,
            HangmanGameRepository hangmanGameRepository,
            GamesRepository gamesRepository,
            PlayerRepository playerRepository) {
        this.STATUS = Status.STARTING;
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userIdWithDiscord = String.format("<@%s>", userId);
        this.guesses = new LinkedHashSet<>();
        this.messageList = new LinkedList<>();
        this.secondPlayer = 0L;
    }

    Hangman(long userId, long secondPlayer, Long guildId, Long channelId,
            HangmanGameRepository hangmanGameRepository,
            GamesRepository gamesRepository,
            PlayerRepository playerRepository) {
        this.STATUS = Status.STARTING;
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userIdWithDiscord = String.format("<@%s>\n<@%s>", userId, secondPlayer);
        this.guesses = new LinkedHashSet<>();
        this.messageList = new LinkedList<>();
        this.secondPlayer = secondPlayer;
    }

    Hangman(long userId, long secondPlayer, Long guildId, Long channelId,
            String guesses, String word, String WORD_HIDDEN,
            int hangmanErrors, LocalDateTime localDateTime,
            HangmanGameRepository hangmanGameRepository,
            GamesRepository gamesRepository,
            PlayerRepository playerRepository) {
        this.STATUS = Status.STARTING;
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;

        if (secondPlayer != 0) {
            this.userIdWithDiscord = String.format("<@%s>\n<@%s>", userId, secondPlayer);
        } else {
            this.userIdWithDiscord = String.format("<@%s>", userId);
        }

        this.guesses = new LinkedHashSet<>();
        this.messageList = new LinkedList<>();
        this.secondPlayer = secondPlayer;
        //Обновить параметры
        if (guesses != null) {
            this.guesses.addAll(Arrays.asList(guesses.split(", ")));
        }
        this.WORD = word;
        this.WORD_HIDDEN = WORD_HIDDEN;
        this.hangmanErrors = hangmanErrors;
        this.WORD_OF_CHARS = word.split("");
        setTimer(localDateTime);
        autoInsert();
        autoDeletingMessages();
    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        String gameLanguage = BotStartConfig.getMapGameLanguages().get(userId);

        if (gameLanguage == null) {
            EmbedBuilder needSetLanguage = new EmbedBuilder();

            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId);

            needSetLanguage.setAuthor(userName, null, avatarUrl);
            needSetLanguage.setColor(Color.GREEN);
            needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

            textChannel.sendMessageEmbeds(needSetLanguage.build())
                    .addActionRow(ButtonIMpl.BUTTON_RUSSIAN, ButtonIMpl.BUTTON_ENGLISH)
                    .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                    .queue();

            HangmanRegistry.getInstance().removeHangman(userId);
            return;
        }

        GameWordLanguage gameWordLanguage = new GameWordLanguage();
        gameWordLanguage.setLanguage(gameLanguage);

        if (BotStartConfig.mapGameCategory.get(userId) != null) {
            gameWordLanguage.setCategory(BotStartConfig.mapGameCategory.get(userId));
        }

        try {
            WORD = megoruAPI.getWord(gameWordLanguage).getWord();
            if (WORD != null && WORD.length() > 0) {
                WORD_OF_CHARS = WORD.split(""); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            } else {
                throw new IllegalArgumentException(WORD);
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

    public void fullWord(final String inputs, final Message messages) {
        try {
            messageList.add(messages);
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
                if (STATUS == Status.SAME_LETTER) return;
                STATUS = Status.SAME_LETTER;

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
                STATUS = Status.WIN_GAME;
                gameWin(inputs);
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    STATUS = Status.LOSE_GAME;
                    gameLose(inputs);
                } else {
                    STATUS = Status.WRONG_WORD;
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

            if (secondPlayer != 0L) {
                HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId, secondPlayer));
            } else {
                HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId));
            }

            ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, true);
            resultGame.send();

            if (secondPlayer != 0L) {
                ResultGame resultGameSecondPlayer = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, secondPlayer, true);
                resultGameSecondPlayer.send();
            }

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

            if (secondPlayer != 0L) {
                HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId, secondPlayer));
            } else {
                HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId));
            }

            ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, false);
            resultGame.send();

            if (secondPlayer != 0L) {
                ResultGame resultGameSecondPlayer = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, secondPlayer, false);
                resultGameSecondPlayer.send();
            }

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
                    if (STATUS == Status.SAME_LETTER) return;
                    STATUS = Status.SAME_LETTER;

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
                    STATUS = Status.RIGHT_LETTER;
                    String result = replacementLetters(inputs);
                    //Игрок угадал все буквы
                    if (!result.contains("_")) {
                        STATUS = Status.WIN_GAME;
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
                        STATUS = Status.LOSE_GAME;
                        gameLose(inputs);
                    } else {
                        STATUS = Status.WRONG_LETTER;
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

    private String category() {
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

    public EmbedBuilder embedBuilder(Color color, String gameInfo, boolean gameGuesses, boolean isDefeat, @Nullable String inputs) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        System.out.println("BotStartConfig.getMapGameLanguages().get(userId): " + BotStartConfig.getMapGameLanguages().get(userId));
        String language = BotStartConfig.getMapGameLanguages().get(userId).equals("rus")
                ? "Кириллица\nКатег.: " + category()
                : "Latin\nCateg.:" + category();

        LOGGER.info("\ngamePlayer: " + userIdWithDiscord
                + "\ngameInfo: " + gameInfo
                + "\ngameGuesses: " + gameGuesses
                + "\nisDefeat: " + isDefeat
                + "\ninputs: " + inputs
                + "\nlanguage " + language);

        String gamePlayer;
        if (secondPlayer == 0L) {
            gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
        } else {
            gamePlayer = jsonGameParsers.getLocale("Game_Players", userId);
        }

        String gameLanguage = jsonGameParsers.getLocale("Game_Language", userId);

        embedBuilder.setColor(color);
        embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
        embedBuilder.addField(gameLanguage, language, true);
        embedBuilder.setThumbnail(ImageURL.get(hangmanErrors));

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
            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(userId);
            activeHangman.setSecondUserIdLong(secondPlayer == 0L ? null : secondPlayer);
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

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            if (Objects.equals(WORD_OF_CHARS[i], "-")) {
                sb.append(" -");
            } else if (Objects.equals(WORD_OF_CHARS[i], " ")) {
                sb.append("  ");
            } else {
                sb.append(sb.length() == 0 ? "_" : " _");
            }
            i++;
        }
        WORD_HIDDEN = sb.toString();
    }

    //заменяет "_" на букву которая есть в слове
    private String replacementLetters(String letter) {
        try {
            StringBuilder sb = new StringBuilder(WORD_HIDDEN);
            for (int i = 0; i < WORD_OF_CHARS.length; i++) {
                if (WORD_OF_CHARS[i].equals(letter)) {
                    sb.replace(
                            i == 0 ? i : i * 2,
                            i == 0 ? i + 1 : i * 2 + 1,
                            String.valueOf(WORD_OF_CHARS[i]));
                }
            }
            WORD_HIDDEN = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }
        return WORD_HIDDEN;
    }

    private synchronized boolean isLetterPresent(final String inputs) {
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

    public long getSecondPlayer() {
        return secondPlayer;
    }

    public long getUserId() {
        return userId;
    }

    public int getLengthWord() {
        return WORD != null ? WORD.length() : 0;
    }

    public Timer getAutoDeletingTimer() {
        return autoDeletingTimer;
    }

    public Timer getAutoInsert() {
        return autoInsert;
    }

    public Timer getStopHangmanTimer() {
        return stopHangmanTimer;
    }

    private void insert() {
        try {
            if ((guesses.size() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(userId)) {
                countUsedLetters = guesses.size();
                hangmanGameRepository.updateGame(userId, WORD_HIDDEN, getGuesses(), hangmanErrors);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void setTimer(LocalDateTime ldt) {
        StopHangmanTimer stopHangman = new StopHangmanTimer();
        Timestamp timestamp = Timestamp.valueOf(ldt.atZone(ZoneOffset.UTC).toLocalDateTime().plusMinutes(10));
        Date date = new Date(timestamp.getTime());
        stopHangmanTimer.schedule(stopHangman, date);
    }

    //Автоматически отправляет в БД данные
    private void autoInsert() {
        AutoUpdate autoUpdate = new AutoUpdate();
        autoInsert.scheduleAtFixedRate(autoUpdate, 7000, 5000);
    }

    //Автоматически отправляет в БД данные
    private void autoDeletingMessages() {
        AutoDeletingMessages autoDeletingMessages = new AutoDeletingMessages();
        autoDeletingTimer.scheduleAtFixedRate(autoDeletingMessages, 7000, 5000);
    }

    private final class AutoDeletingMessages extends TimerTask {

        @Override
        public void run() {
            try {
                if (guildId == null) {
                    autoDeletingTimer.cancel();
                    return;
                }

                Guild guildById = BotStartConfig.jda.getGuildById(guildId);

                if (guildById != null) {
                    Member selfMember = guildById.getSelfMember();
                    TextChannel textChannelById = BotStartConfig.jda.getTextChannelById(channelId);

                    if (textChannelById != null) {
                        if (selfMember.hasPermission(textChannelById, Permission.MESSAGE_MANAGE) && messageList.size() >= 3) {
                            String format =
                                    String.format("\nAutoDeletingMessages: %s\nmessageList: %s",
                                            messageList.size(),
                                            Arrays.toString(messageList.toArray()));

                            LOGGER.info(format);
                            textChannelById.deleteMessages(messageList).submit().get();
                            //Так как метод асинхронный иногда может возникать NPE
                            messageList.clear();
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
            insert();
        }

    }

    private final class StopHangmanTimer extends TimerTask {

        @Override
        public void run() {
            STATUS = Status.TIME_OVER;
            try {
                if (HangmanRegistry.getInstance().hasHangman(userId)) {
                    String gameOver = jsonGameParsers.getLocale("gameOver", userId);
                    String timeIsOver = jsonGameParsers.getLocale("timeIsOver", userId);
                    String gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);

                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(Color.GREEN);
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
    }

    private enum Status {
        WRONG_LETTER,
        SAME_LETTER,
        RIGHT_LETTER,
        WRONG_WORD,
        WIN_GAME,
        LOSE_GAME,
        TIME_OVER,
        STARTING
    }
}