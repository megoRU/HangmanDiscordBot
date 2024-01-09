package main.game;

import lombok.Getter;
import lombok.Setter;
import main.controller.UpdateController;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
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
    private final UpdateController updateController;

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
    @Setter
    private Long againstPlayerId;
    private long channelId;
    private long messageId;

    @Setter
    private volatile GameStatus gameStatus;

    @Autowired
    public Hangman(UpdateController updateController,
                   HangmanGameRepository hangmanGameRepository,
                   HangmanDataSaving hangmanDataSaving,
                   HangmanResult hangmanResult) {
        this.updateController = updateController;
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
        this.isCompetitive = isCompetitive;
        this.gameStatus = GameStatus.STARTING;
        this.messageId = messageId;
        this.hangmanPlayers = hangmanPlayers;

        //Люблю кастыли
        if (isCompetitive) {
            channelId = hangmanPlayers[0].getChannelId();
        }

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
        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        long userId = hangmanPlayer.getUserId();

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
        channelId = hangmanDataSaving.saveGame(this);

        //Установка авто завершения
        setTimer(LocalDateTime.now());
    }

    public void startGame(MessageChannel textChannel) {
        HangmanPlayer hangmanPlayer = hangmanPlayers[0];
        long userId = hangmanPlayer.getUserId();

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
        channelId = hangmanDataSaving.saveGame(this);

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

            String gameStopWin = JSON_GAME_PARSERS.getLocale("Game_Stop_Win", userId);
            String gameYouLose = JSON_GAME_PARSERS.getLocale("Game_You_Lose", userId);
            String gameCompetitiveYouWin = JSON_GAME_PARSERS.getLocale("Game_Competitive_You_Win", userId);
            String gameCompetitiveYouLose = JSON_GAME_PARSERS.getLocale("Game_Competitive_You_Lose", userId);

            //Чтобы было показано слово которое было
            HangmanRegistry instance = HangmanRegistry.getInstance();
            if (result) {
                instance.setHangmanStatus(againstPlayerId, GameStatus.LOSE_GAME);
            } else {
                instance.setHangmanStatus(againstPlayerId, GameStatus.WIN_GAME);
            }

            EmbedBuilder win = HangmanEmbedUtils.hangmanLayout(userId, gameStopWin);
            EmbedBuilder lose = HangmanEmbedUtils.hangmanLayout(userId, gameYouLose);
            EmbedBuilder competitiveWin = HangmanEmbedUtils.hangmanLayout(againstPlayerId, gameCompetitiveYouWin);
            EmbedBuilder competitiveLose = HangmanEmbedUtils.hangmanLayout(againstPlayerId, gameCompetitiveYouLose);

            if (hangmanPlayers.length == 1) {
                HangmanEmbedUtils.editMessageWithButtons(result ? win : lose, isCompetitive, userId, HangmanUtils.getListButtons(userId), updateController.getHangmanGameRepository());

                if (isCompetitive) {
                    HangmanEmbedUtils.editMessageWithButtons(!result ? competitiveWin : competitiveLose, true, againstPlayerId, HangmanUtils.getListButtons(againstPlayerId), updateController.getHangmanGameRepository());
                }
            } else {
                HangmanPlayer hangmanPlayerSecond = hangmanPlayers[1];
                long secondUserId = hangmanPlayerSecond.getUserId();
                HangmanEmbedUtils.editMessageWithButtons(result ? win : lose, isCompetitive, userId, HangmanUtils.getListButtons(userId, secondUserId), updateController.getHangmanGameRepository());
            }

            //Люблю кастыли
            if (isCompetitive) {
                hangmanResult.save(hangmanPlayers, result, true);

                HangmanPlayer hangmanSecondPlayer = new HangmanPlayer(againstPlayerId, null, channelId);
                HangmanPlayer[] hangmanPlayers = new HangmanPlayer[]{hangmanSecondPlayer};
                hangmanResult.save(hangmanPlayers, !result, true);

                HangmanRegistry.getInstance().removeHangman(userId);
                HangmanRegistry.getInstance().removeHangman(againstPlayerId);
            } else {
                hangmanResult.save(hangmanPlayers, result, false);

                HangmanRegistry.getInstance().removeHangman(userId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in gameEnd", e);
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
            return String.format("<@%s>\n<@%s>", hangmanPlayers[0].getUserId(), hangmanPlayers[1].getUserId());
        } else {
            return String.format("<@%s>", hangmanPlayers[0].getUserId());
        }
    }

    String getAgainstPlayerWithDiscord() {
        return String.format("<@%s>", againstPlayerId);
    }
}