package main.game;

import lombok.Getter;
import lombok.Setter;
import main.enums.GameStatus;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

@Getter
@Service
public class Hangman {

    //Logger
    private static final Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    private final static int AUTO_FINISH_MINUTES = 10;

    //Localisation
    private static final JSONParsers JSON_GAME_PARSERS = new JSONParsers(JSONParsers.Locale.GAME);
    private static final JSONParsers JSON_BOT_PARSERS = new JSONParsers(JSONParsers.Locale.BOT);

    //Service
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;

    private final HangmanAPI hangmanAPI = new HangmanAPI();
    private final Set<String> guesses;
    @Setter
    private HangmanPlayer[] hangmanPlayers;

    @Setter
    private int usedLettersCount;
    private String WORD;
    private String[] WORD_OF_CHARS;
    private String WORD_HIDDEN;
    private int hangmanErrors;
    @Setter
    private boolean isCompetitive;
    @Nullable
    private Long againstPlayerId;
    private long againstPlayerEmbedded;
    private long channelId;
    private long messageId;

    @Setter
    private volatile GameStatus gameStatus;

    @Autowired
    public Hangman(HangmanGameRepository hangmanGameRepository,
                   HangmanDataSaving hangmanDataSaving,
                   HangmanResult hangmanResult) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.hangmanDataSaving = hangmanDataSaving;
        this.hangmanResult = hangmanResult;
        this.guesses = new LinkedHashSet<>();
        this.gameStatus = GameStatus.STARTING;
    }

    Hangman update(long messageId,
                   String guesses,
                   String word,
                   String WORD_HIDDEN,
                   int hangmanErrors,
                   LocalDateTime localDateTime,
                   boolean isCompetitive,
                   Long againstPlayerId,
                   HangmanPlayer... hangmanPlayers) {
        this.againstPlayerId = againstPlayerId;
        this.againstPlayerEmbedded = againstPlayerId;
        this.isCompetitive = isCompetitive;
        this.gameStatus = GameStatus.STARTING;
        this.messageId = messageId;
        this.hangmanPlayers = hangmanPlayers;
        this.channelId = hangmanPlayers[0].getChannelId();
        //Обновить параметры
        if (guesses != null) {
            this.guesses.addAll(Arrays.asList(guesses.split(", ")));
        }
        this.WORD = word;
        this.WORD_HIDDEN = WORD_HIDDEN;
        this.hangmanErrors = hangmanErrors;
        this.WORD_OF_CHARS = word.split("");
        setTimer(localDateTime);
        return this;
    }

    public void startGame(MessageChannel textChannel, String word) {
        long userId = HangmanUtils.getHangmanFirstPlayer(hangmanPlayers);

        try {
            WORD = word;
            WORD_OF_CHARS = WORD.toLowerCase().split(""); // Преобразуем строку str в массив символов (char)
            hideWord(WORD.length());
        } catch (Exception e) {
            HangmanUtils.handleAPIException(userId, textChannel);
            return;
        }

        String gameStart = JSON_GAME_PARSERS.getLocale("Game_Start", userId);
        EmbedBuilder start = HangmanEmbedUtils.hangmanLayout(userId, gameStart);
        messageId = textChannel.sendMessageEmbeds(start.build()).complete().getIdLong();
        hangmanDataSaving.saveGame(this);

        //Установка авто завершения
        setTimer(LocalDateTime.now());
    }

    public void startGame(MessageChannel textChannel) {
        long userId = HangmanUtils.getHangmanFirstPlayer(hangmanPlayers);

        try {
            WORD = hangmanAPI.getWord(userId);
            WORD_OF_CHARS = WORD.toLowerCase().split(""); // Преобразуем строку str в массив символов (char)
            hideWord(WORD.length());
        } catch (Exception e) {
            HangmanUtils.handleAPIException(userId, textChannel);
            return;
        }

        String gameStart = JSON_GAME_PARSERS.getLocale("Game_Start", userId);
        EmbedBuilder start = HangmanEmbedUtils.hangmanLayout(userId, gameStart);
        messageId = textChannel.sendMessageEmbeds(start.build()).complete().getIdLong();
        hangmanDataSaving.saveGame(this);

        //Установка авто завершения
        setTimer(LocalDateTime.now());
    }

    public synchronized void inputHandler(@NotNull final String inputs, @NotNull final Message messages) {
        HangmanInputs hangmanInputs = new HangmanInputs(hangmanGameRepository);
        hangmanInputs.handler(inputs, messages, this);
    }

    void gameEnd(boolean result) {
        HangmanGameEndHandler hangmanGameEndHandler = new HangmanGameEndHandler(hangmanGameRepository);
        hangmanGameEndHandler.handleGameEnd(this, result);
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
                sb.append(sb.isEmpty() ? "_" : " _");
            }
            i++;
        }
        WORD_HIDDEN = sb.toString();
    }

    //заменяет "_" на букву которая есть в слове
    synchronized String replacementLetters(String letter) {
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

    synchronized void incrementHangmanErrors() {
        this.hangmanErrors++;
    }

    public int getGuessesSize() {
        return guesses.size();
    }

    public int getLengthWord() {
        return WORD != null ? WORD.length() : 0;
    }

    private void setTimer(LocalDateTime ldt) {
        Timestamp timestamp = Timestamp.valueOf(ldt.atZone(ZoneOffset.UTC).toLocalDateTime().plusMinutes(AUTO_FINISH_MINUTES));
        HangmanRegistry.getInstance().setHangmanTimer(this, timestamp);
    }

    String getUserIdWithDiscord() {
        if (hangmanPlayers.length > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            for (HangmanPlayer hangmanPlayer : hangmanPlayers) {
                stringBuilder.append(String.format("<@%s>\n", hangmanPlayer.getUserId()));
            }
            return stringBuilder.toString();
        } else {
            return String.format("<@%s>", hangmanPlayers[0].getUserId());
        }
    }

    String getAgainstPlayerWithDiscord() {
        return String.format("<@%s>", againstPlayerEmbedded);
    }

    public int getPlayersCount() {
        return hangmanPlayers.length;
    }

    public void deleteAgainstPlayer() {
        this.againstPlayerId = null;
    }

    public void setAgainstPlayerId(long againstPlayerId) {
        this.againstPlayerId = againstPlayerId;
        this.againstPlayerEmbedded = againstPlayerId;
    }
}