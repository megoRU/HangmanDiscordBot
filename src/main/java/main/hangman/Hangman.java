package main.hangman;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPI;
import lombok.Getter;
import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

@Getter
public class Hangman {

    //Logger
    private final Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    //Localisation
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    //UpdateController
    private final UpdateController updateController;

    //API
    private final MegoruAPI megoruAPI = new MegoruAPI.Builder().build();

    private final Set<String> guesses;
    private final HangmanPlayer[] hangmanPlayers;
    private long messageId;

    private int usedLettersCount;
    private String WORD;
    private String[] WORD_OF_CHARS;
    private String WORD_HIDDEN;
    private int hangmanErrors;

    private volatile Status STATUS;

    Hangman(UpdateController updateController, HangmanPlayer... hangmanPlayers) {
        this.STATUS = Status.STARTING;
        this.updateController = updateController;
        this.guesses = new LinkedHashSet<>();
        this.hangmanPlayers = hangmanPlayers;
    }

    Hangman(long messageId,
            String guesses,
            String word,
            String WORD_HIDDEN,
            int hangmanErrors,
            LocalDateTime localDateTime,
            UpdateController updateController,
            HangmanPlayer... hangmanPlayers) {
        this.STATUS = Status.STARTING;
        this.updateController = updateController;
        this.messageId = messageId;
        this.hangmanPlayers = hangmanPlayers;
        this.guesses = new LinkedHashSet<>();

        //Обновить параметры
        if (guesses != null) {
            this.guesses.addAll(Arrays.asList(guesses.split(", ")));
        }
        this.WORD = word;
        this.WORD_HIDDEN = WORD_HIDDEN;
        this.hangmanErrors = hangmanErrors;
        this.WORD_OF_CHARS = word.split("");
        setTimer(localDateTime);
    }

    //TODO: Сделать проверку в классах что вызывают на наличие языка
    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        long userId = hangmanPlayer.getUserId();

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
    }

    public void inputHandler(@NotNull final String inputs, @NotNull final Message messages) {
        HangmanInputs hangmanInputs = new HangmanInputs(this, updateController);
        hangmanInputs.handler(inputs, messages);
    }

    void gameEnd(boolean result) {
        try {
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];
            long userId = hangmanPlayer.getUserId();

            String gameStopWin = jsonGameParsers.getLocale("Game_Stop_Win", userId);
            EmbedBuilder win = HangmanEmbedUtils.hangmanPattern(userId, gameStopWin);

            if (hangmanPlayers.length == 1) {
                HangmanEmbedUtils.editMessageWithButtons(win, userId, HangmanUtils.getListButtons(userId), updateController.getHangmanGameRepository());
            } else {
                HangmanPlayer hangmanPlayerSecond = hangmanPlayers[1];
                long secondUserId = hangmanPlayerSecond.getUserId();
                HangmanEmbedUtils.editMessageWithButtons(win, userId, HangmanUtils.getListButtons(userId, secondUserId), updateController.getHangmanGameRepository());
            }

            HangmanResult hangmanResult = new HangmanResult(hangmanPlayers, result, updateController);
            hangmanResult.save();

            HangmanRegistry.getInstance().removeHangman(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createEntityInDataBase() {
        try {
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];
            long userId = hangmanPlayer.getUserId();
            long guildId = hangmanPlayer.getGuildId();
            long channelId = hangmanPlayer.getChannelId();

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(userId);

            if (hangmanPlayers.length > 1) {
                activeHangman.setSecondUserIdLong(hangmanPlayers[1].getUserId());
            }

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
    void hideWord(int length) {
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
    String replacementLetters(String letter) {
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

    synchronized boolean isLetterPresent(final String inputs) {
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

    synchronized void incrementHangmanErrors() {
        this.hangmanErrors++;
    }

    public int getGuessesSize() {
        return guesses.size();
    }

    public void setUsedLettersCount(int number) {
        usedLettersCount = number;
    }

    public int getLengthWord() {
        return WORD != null ? WORD.length() : 0;
    }

    public void setSTATUS(Status status) {
        this.STATUS = status;
    }

    private void setTimer(LocalDateTime ldt) {
        Timestamp timestamp = Timestamp.valueOf(ldt.atZone(ZoneOffset.UTC).toLocalDateTime().plusMinutes(10));
        HangmanRegistry.getInstance().setHangmanTimer(this, timestamp);
    }

    String getUserIdWithDiscord() {
        if (hangmanPlayers.length > 1) {
            return String.format("<@%s>\n<@%s>", hangmanPlayers[0].getUserId(), hangmanPlayers[1].getUserId());
        } else {
            return String.format("<@%s>", hangmanPlayers[0].getUserId());
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