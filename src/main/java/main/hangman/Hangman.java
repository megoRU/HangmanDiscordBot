package main.hangman;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPI;
import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.hangman.impl.EndGameButtons;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.junit.internal.Checks;

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
    private final UpdateController updateController;

    //API
    private final MegoruAPI megoruAPI = new MegoruAPI.Builder().build();

    private final Set<String> guesses;

    private final List<Message> messageList;

    //User|Guild|Channel data
    private final long userId;
    private final long secondPlayer;
    private final Long guildId;
    private final Long channelId;
    private final String userIdWithDiscord;
    private long messageId;

    private int countUsedLetters;
    private String WORD;
    private String[] WORD_OF_CHARS;
    private String WORD_HIDDEN;
    private int hangmanErrors;

    private volatile Status STATUS;

    Hangman(long userId, Long guildId, Long channelId, UpdateController updateController) {
        this.STATUS = Status.STARTING;
        this.updateController = updateController;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userIdWithDiscord = String.format("<@%s>", userId);
        this.guesses = new LinkedHashSet<>();
        this.messageList = new LinkedList<>();
        this.secondPlayer = 0L;
    }

    Hangman(long userId, long secondPlayer, Long guildId, Long channelId, UpdateController updateController) {
        this.STATUS = Status.STARTING;
        this.updateController = updateController;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userIdWithDiscord = String.format("<@%s>\n<@%s>", userId, secondPlayer);
        this.guesses = new LinkedHashSet<>();
        this.messageList = new LinkedList<>();
        this.secondPlayer = secondPlayer;
    }

    Hangman(long userId,
            long secondPlayer,
            Long guildId,
            Long channelId,
            long messageId,
            String guesses,
            String word,
            String WORD_HIDDEN,
            int hangmanErrors,
            LocalDateTime localDateTime,
            UpdateController updateController) {
        this.STATUS = Status.STARTING;
        this.updateController = updateController;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;

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

    //TODO: Сделать проверку в классах что вызывают на наличие языка
    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        String gameLanguage = BotStartConfig.getMapGameLanguages().get(userId);
        GameWordLanguage gameWordLanguage = new GameWordLanguage();
        gameWordLanguage.setLanguage(gameLanguage);

        if (BotStartConfig.mapGameCategory.get(userId) != null) {
            gameWordLanguage.setCategory(BotStartConfig.mapGameCategory.get(userId));
        }

        try {
            WORD = megoruAPI.getWord(gameWordLanguage).getWord();
            if (WORD != null && WORD.length() > 0) {
                WORD_OF_CHARS = WORD.toLowerCase().split(""); // Преобразуем строку str в массив символов (char)
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
        EmbedBuilder start = HangmanEmbedUtils.hangmanPattern(userId, gameStart);
        messageId = textChannel.sendMessageEmbeds(start.build()).complete().getIdLong();
        createEntityInDataBase();

        //Установка авто завершения
        setTimer(LocalDateTime.now());
        autoInsert();
        autoDeletingMessages();
    }

    public void inputHandler(@NotNull final String inputs, @NotNull final Message messages) {
        messageList.add(messages);
        try {
            Checks.notNull(WORD);
        } catch (NullPointerException e) {
            String wordIsNull = jsonParsers.getLocale("word_is_null", userId);
            messages.getChannel()
                    .sendMessage(wordIsNull)
                    .setActionRow(EndGameButtons.getListButtons(userId))
                    .queue();

            HangmanRegistry.getInstance().removeHangman(userId);
            e.printStackTrace();
            return;
        }
        try {
            if (inputs.length() == 1) {
                if (WORD_HIDDEN.contains("_")) {
                    if (isLetterPresent(inputs.toUpperCase())) {
                        if (STATUS == Status.SAME_LETTER) return;
                        STATUS = Status.SAME_LETTER;
                        String gameYouUseThisLetter = jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId);
                        EmbedBuilder info = HangmanEmbedUtils.hangmanPattern(userId, gameYouUseThisLetter);
                        HangmanHelper.editMessage(info, userId, updateController.getHangmanGameRepository());
                        return;
                    }

                    if (WORD.contains(inputs)) {
                        STATUS = Status.RIGHT_LETTER;
                        String result = replacementLetters(inputs);
                        //Игрок угадал все буквы
                        if (!result.contains("_")) {
                            STATUS = Status.WIN_GAME;
                            gameWin();
                            return;
                        }
                        String gameYouGuessLetter = jsonGameParsers.getLocale("Game_You_Guess_Letter", userId);
                        EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userId, gameYouGuessLetter);
                        HangmanHelper.editMessage(embedBuilder, userId, updateController.getHangmanGameRepository());
                    } else {
                        hangmanErrors++;
                        if (hangmanErrors >= 8) {
                            STATUS = Status.LOSE_GAME;
                            gameLose();
                        } else {
                            STATUS = Status.WRONG_LETTER;
                            String gameNoSuchLetter = jsonGameParsers.getLocale("Game_No_Such_Letter", userId);
                            EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanPattern(userId, gameNoSuchLetter);
                            HangmanHelper.editMessage(wordNotFound, userId, updateController.getHangmanGameRepository());
                        }
                    }
                }
            } else {
                if (inputs.length() != getLengthWord()) {
                    String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userId);
                    EmbedBuilder wrongLength = HangmanEmbedUtils.hangmanPattern(userId, wrongLengthJson);
                    HangmanHelper.editMessage(wrongLength, userId, updateController.getHangmanGameRepository());
                    return;
                }

                if (isLetterPresent(inputs)) {
                    if (STATUS == Status.SAME_LETTER) return;
                    STATUS = Status.SAME_LETTER;
                    String gameYouUseThisWord = jsonGameParsers.getLocale("Game_You_Use_This_Word", userId);
                    EmbedBuilder info = HangmanEmbedUtils.hangmanPattern(userId, gameYouUseThisWord);
                    HangmanHelper.editMessage(info, userId, updateController.getHangmanGameRepository());
                    return;
                }

                if (inputs.equals(WORD)) {
                    STATUS = Status.WIN_GAME;
                    gameWin();
                } else {
                    hangmanErrors++;
                    if (hangmanErrors >= 8) {
                        STATUS = Status.LOSE_GAME;
                        gameLose();
                    } else {
                        STATUS = Status.WRONG_WORD;
                        String gameNoSuchWord = jsonGameParsers.getLocale("Game_No_Such_Word", userId);
                        EmbedBuilder wordNotFound = HangmanEmbedUtils.hangmanPattern(userId, gameNoSuchWord);
                        HangmanHelper.editMessage(wordNotFound, userId, updateController.getHangmanGameRepository());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }

    }

    private void gameWin() {
        try {
            String gameStopWin = jsonGameParsers.getLocale("Game_Stop_Win", userId);
            EmbedBuilder win = HangmanEmbedUtils.hangmanPattern(userId, gameStopWin);

            if (secondPlayer != 0L) {
                HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId, secondPlayer), updateController.getHangmanGameRepository());
            } else {
                HangmanHelper.editMessageWithButtons(win, userId, EndGameButtons.getListButtons(userId), updateController.getHangmanGameRepository());
            }

            ResultGame resultGame = new ResultGame(updateController.getHangmanGameRepository(), updateController.getGamesRepository(), updateController.getPlayerRepository(), userId, true);
            resultGame.send();

            if (secondPlayer != 0L) {
                ResultGame resultGameSecondPlayer = new ResultGame(updateController.getHangmanGameRepository(), updateController.getGamesRepository(), updateController.getPlayerRepository(), secondPlayer, true);
                resultGameSecondPlayer.send();
            }

            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void gameLose() {
        try {
            String gameYouLose = jsonGameParsers.getLocale("Game_You_Lose", userId);
            EmbedBuilder info = HangmanEmbedUtils.hangmanPattern(userId, gameYouLose);
            if (secondPlayer != 0L) {
                HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId, secondPlayer), updateController.getHangmanGameRepository());
            } else {
                HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId), updateController.getHangmanGameRepository());
            }

            ResultGame resultGame = new ResultGame(updateController.getHangmanGameRepository(), updateController.getGamesRepository(), updateController.getPlayerRepository(), userId, false);
            resultGame.send();

            if (secondPlayer != 0L) {
                ResultGame resultGameSecondPlayer = new ResultGame(updateController.getHangmanGameRepository(), updateController.getGamesRepository(), updateController.getPlayerRepository(), secondPlayer, false);
                resultGameSecondPlayer.send();
            }

            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void createEntityInDataBase() {
        try {
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(userId);
            activeHangman.setSecondUserIdLong(secondPlayer == 0L ? null : secondPlayer);
            activeHangman.setMessageIdLong(messageId);
            activeHangman.setChannelIdLong(channelId);
            activeHangman.setGuildLongId(guildId);
            activeHangman.setWord(WORD);
            activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
            activeHangman.setHangmanErrors(hangmanErrors);
            activeHangman.setGameCreatedTime(timestamp);
            updateController.getHangmanGameRepository().saveAndFlush(activeHangman);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            if (Objects.equals(WORD_OF_CHARS[i], "-")
                    || Objects.equals(WORD_OF_CHARS[i], "–")
                    || Objects.equals(WORD_OF_CHARS[i], "—")) {
                sb.append(" —");
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

    public long getMessageId() {
        return messageId;
    }

    public String getUserIdWithDiscord() {
        return userIdWithDiscord;
    }

    public int getHangmanErrors() {
        return hangmanErrors;
    }

    public String getWORD_HIDDEN() {
        return WORD_HIDDEN;
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

    public String getWORD() {
        return WORD;
    }

    public Status getSTATUS() {
        return STATUS;
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
//                            String format =
//                                    String.format("\nAutoDeletingMessages: %s\nmessageList: %s",
//                                            messageList.size(),
//                                            Arrays.toString(messageList.toArray()));

//                            LOGGER.info(format);
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
            try {
                if ((guesses.size() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(userId)) {
                    countUsedLetters = guesses.size();
                    updateController.getHangmanGameRepository().updateGame(userId, WORD_HIDDEN, getGuesses(), hangmanErrors);
                }
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
            }
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

                    HangmanHelper.editMessageWithButtons(info, userId, EndGameButtons.getListButtons(userId), updateController.getHangmanGameRepository());
                    updateController.getHangmanGameRepository().deleteActiveGame(userId);
                    HangmanRegistry.getInstance().removeHangman(userId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public enum Status {
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