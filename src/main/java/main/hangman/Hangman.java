package main.hangman;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPIImpl;
import main.config.BotStartConfig;
import main.hangman.impl.EndGameButtons;
import main.hangman.impl.GetImage;
import main.hangman.impl.HangmanHelper;
import main.hangman.impl.SetGameLanguageButtons;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    private final Set<String> guesses = new LinkedHashSet<>();

    //User|Guild|Channel data
    private final String userId;
    private final String guildId;
    private final Long channelId;

    private final List<Message> messageList = new LinkedList<>();
    private int countUsedLetters;
    private String WORD;
    private String[] wordToChar;
    private String WORD_HIDDEN;
    private String currentHiddenWord;
    private int hangmanErrors;

    public Hangman(String userId, String guildId, Long channelId,
                   HangmanGameRepository hangmanGameRepository,
                   GamesRepository gamesRepository,
                   PlayerRepository playerRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    //TODO: Работает, но изменить время на Instant желательно.
    private EmbedBuilder updateEmbedBuilder() {
        autoInsert();

        Instant instant = Instant.now().plusSeconds(600L);
        HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(Instant.now()))));
        HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId), String.valueOf(OffsetDateTime.parse(String.valueOf(instant))));

        return embedBuilder(
                Color.GREEN,
                "<@" + Long.parseLong(userId) + ">",
                jsonGameParsers.getLocale("Game_Start", userId),
                false,
                false,
                null
        );
    }

    public void startGame(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(userId) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                needSetLanguage.setColor(Color.GREEN);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                event.replyEmbeds(needSetLanguage.build()).addActionRow(SetGameLanguageButtons.getList).queue();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
                return;
            }

            MegoruAPI megoruAPI = new MegoruAPIImpl("this bot don`t use token");
            GameWordLanguage gameWordLanguage = new GameWordLanguage();
            gameWordLanguage.setLanguage(BotStartConfig.getMapGameLanguages().get(userId));

            try {
                WORD = megoruAPI.getWord(gameWordLanguage).getWord();
                if (WORD != null) {
                    wordToChar = WORD.split("");
                    hideWord(WORD.length());
                }
            } catch (Exception e) {
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", event.getUser().getId()));
                wordIsNull.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", event.getUser().getId()));

                event.replyEmbeds(wordIsNull.build()).queue();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
                LOGGER.info(e.getMessage());
                return;
            }

            event.replyEmbeds(updateEmbedBuilder().build()).queue(m -> m.retrieveOriginal().queue(this::createEntityInDataBase));
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(userId) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(userName, null, avatarUrl);
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", this.userId));

                textChannel.sendMessageEmbeds(needSetLanguage.build()).setActionRow(SetGameLanguageButtons.getList).queue();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
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
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", userId));
                wordIsNull.setAuthor(userName, null, avatarUrl);
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", userId));

                textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
                return;
            }

            textChannel.sendMessageEmbeds(updateEmbedBuilder().build()).queue(this::createEntityInDataBase);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    //TODO: Возможно произойдет так что игру закончили. Удалили данные из БД и произойдет REPLACE и игра не завершится
    private void executeInsert() {
        try {
            if ((guesses.size() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                countUsedLetters = guesses.size();
                System.out.println("currentHiddenWord: " + currentHiddenWord);
                System.out.println("getGuesses(): " + getGuesses());
                hangmanGameRepository.updateGame(Long.valueOf(userId), currentHiddenWord, getGuesses(), hangmanErrors);
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
                    if (HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                        executeInsert();
                        deleteMessages();
                    } else {
                        deleteMessages();
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.info(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }, 7000, 5000);
    }

    private void deleteMessages() {
        try {
            if (guildId == null) return;
            if (BotStartConfig.jda.getGuildById(guildId) == null) {
                hangmanGameRepository.deleteActiveGame(Long.valueOf(userId));
                return;
            }
            if (BotStartConfig.jda
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.jda.getTextChannelById(channelId), Permission.MESSAGE_MANAGE) && !messageList.isEmpty()) {
                if (messageList.size() > 2) {
                    LOGGER.info("messageList.size(): " + messageList.size()
                            + "\nmessageList: " + Arrays.toString(messageList.toArray()));

                    List<Message> temp = new ArrayList<>(messageList);
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessages(messageList).queue();
                    //Так как метод асинхронный иногда может возникать NPE
                    Thread.sleep(5000);
                    messageList.removeAll(temp);
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void fullWord(String inputs, Message messages) {
        try {
            messageList.add(messages);
            if (inputs.length() < WORD.length()) return;
            if (isLetterPresent(inputs.toUpperCase())) {
                EmbedBuilder info = embedBuilder(
                        Color.GREEN,
                        "<@" + Long.parseLong(userId) + ">",
                        jsonGameParsers.getLocale("Game_You_Use_This_Word", userId),
                        true,
                        false,
                        inputs
                );
                HangmanHelper.editMessage(info, Long.parseLong(userId));
                return;
            }

            if (inputs.equals(WORD)) {
                EmbedBuilder win = embedBuilder(
                        Color.GREEN,
                        "<@" + Long.parseLong(userId) + ">",
                        jsonGameParsers.getLocale("Game_Stop_Win", userId),
                        true,
                        false,
                        inputs
                );

                HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, true);
                resultGame.send();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    gameLose(inputs);
                } else {
                    EmbedBuilder wordNotFound = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_No_Such_Word", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    private void gameLose(String inputs) {
        try {
            EmbedBuilder info = embedBuilder(
                    Color.GREEN,
                    "<@" + Long.parseLong(userId) + ">",
                    jsonGameParsers.getLocale("Game_You_Lose", userId),
                    true,
                    true,
                    inputs
            );

            HangmanHelper.editMessageWithButtons(info, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

            ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, false);
            resultGame.send();
            HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void logic(String inputs, Message messages) {
        try {
            messageList.add(messages);
            if (WORD == null) {
                messages.getTextChannel()
                        .sendMessage(jsonParsers.getLocale("word_is_null", userId))
                        .setActionRow(EndGameButtons.getListButtons(userId))
                        .queue();
                HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
                return;
            }
        } catch (Exception e) {
            System.out.println("Word null");
        }
        try {
            if (WORD_HIDDEN.contains("_")) {
                if (isLetterPresent(inputs.toUpperCase())) {

                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                    return;
                }

                if (WORD.contains(inputs)) {
                    String result = replacementLetters(inputs);

                    //Игрок угадал все буквы
                    if (!result.contains("_")) {
                        EmbedBuilder win = embedBuilder(
                                Color.GREEN,
                                "<@" + Long.parseLong(userId) + ">",
                                jsonGameParsers.getLocale("Game_Stop_Win", userId),
                                true,
                                false,
                                result
                        );

                        HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                        ResultGame resultGame = new ResultGame(hangmanGameRepository, gamesRepository, playerRepository, userId, true);
                        resultGame.send();
                        HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
                        return;
                    }

                    //Вы угадали букву!
                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_You_Guess_Letter", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                } else {
                    hangmanErrors++;
                    if (hangmanErrors >= 8) {
                        gameLose(inputs);
                    } else {
                        EmbedBuilder wordNotFound = embedBuilder(
                                Color.GREEN,
                                "<@" + Long.parseLong(userId) + ">",
                                jsonGameParsers.getLocale("Game_No_Such_Letter", userId),
                                true,
                                false,
                                inputs
                        );

                        HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public EmbedBuilder embedBuilder(Color color, String gamePlayer, String gameInfo, boolean gameGuesses, boolean isDefeat, @Nullable String inputs) {
        EmbedBuilder embedBuilder = null;
        try {
            String language = BotStartConfig.getMapGameLanguages().get(userId).equals("rus") ? "Кириллица" : "Latin";
            embedBuilder = new EmbedBuilder();

            LOGGER.info("\ngamePlayer: " + gamePlayer
                    + "\ngameInfo: " + gameInfo
                    + "\ngameGuesses: " + gameGuesses
                    + "\nisDefeat: " + isDefeat
                    + "\ninputs: " + inputs
                    + "\nlanguage " + language);

            embedBuilder.setColor(color);
            embedBuilder.addField(jsonGameParsers.getLocale("Game_Player", userId), gamePlayer, true);
            embedBuilder.addField(jsonGameParsers.getLocale("Game_Language", userId), language, true);
            embedBuilder.setThumbnail(GetImage.get(hangmanErrors));

            if (gameGuesses) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
            }

            if (inputs != null && inputs.length() == 1) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(inputs).toUpperCase() + "`", false);
            }

            if (inputs == null) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
            } else if (inputs.length() >= 3) {
                if (inputs.equals(WORD)) {
                    embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD.toUpperCase().replaceAll("", " ").trim() + "`", false);
                } else {
                    embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
                }
            }

            if (isDefeat) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase().replaceAll("", " ").trim() + "`", false);
            }

            embedBuilder.addField(jsonGameParsers.getLocale("Game_Info", userId), gameInfo, false);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        //embedBuilder.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
        //embedBuilder.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
        return embedBuilder;
    }

    private void createEntityInDataBase(Message message) {
        try {
            HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), message.getId());

            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(Long.valueOf(userId));
            activeHangman.setMessageIdLong(Long.valueOf(message.getId()));
            activeHangman.setChannelIdLong(channelId);
            activeHangman.setGuildLongId(guildId != null ? Long.valueOf(guildId) : null);
            activeHangman.setWord(WORD);
            activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
            activeHangman.setHangmanErrors(hangmanErrors);
            activeHangman.setGameCreatedTime(new Timestamp(Instant.now().toEpochMilli()));
            hangmanGameRepository.saveAndFlush(activeHangman);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void stopGameByTime() {
        try {
            EmbedBuilder info = new EmbedBuilder();
            info.setColor(0x00FF00);
            info.setTitle(jsonGameParsers.getLocale("gameOver", userId));
            info.setDescription(jsonGameParsers.getLocale("timeIsOver", userId));
            info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);

            HangmanHelper.editMessageWithButtons(info, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

            HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }

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
    public void updateVariables(String guesses, String word, String currentHiddenWord, int hangmanErrors) {
        if (this.guesses.isEmpty() && this.WORD == null && this.currentHiddenWord == null && this.hangmanErrors == 0) {
            if (guesses != null) {
                this.guesses.addAll(Arrays.asList(guesses.split(", ")));
            }
            this.WORD = word;
            this.WORD_HIDDEN = currentHiddenWord;
            this.currentHiddenWord = currentHiddenWord;
            this.hangmanErrors = hangmanErrors;
            this.wordToChar = word.split("");
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

    public String getGuildId() {
        return guildId;
    }

    public Long getChannelId() {
        return channelId;
    }
}