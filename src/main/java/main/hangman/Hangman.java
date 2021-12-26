package main.hangman;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.jsonparser.JSONGameParsers;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import main.model.entity.Game;
import main.model.entity.Player;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.awt.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Setter
@Getter
public class Hangman implements HangmanHelper {

    private static final String URL_RU = "http://45.140.167.181:8085/api/russian";
    private static final String URL_EN = "http://45.140.167.181:8085/api/english";
    private static final String HANGMAN_URL = "https://megoru.ru/hangman2/";
    private static final JSONGameParsers jsonGameParsers = new JSONGameParsers();
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;
    private final StringBuilder guesses = new StringBuilder();
    private final List<Integer> index = new ArrayList<>();
    private final String userId;
    private final String guildId;
    private final Long channelId;
    private final List<Message> messageList = new ArrayList<>(20);
    private final List<Button> buttons = new ArrayList<>();
    private int countUsedLetters;
    private String WORD = null;
    private char[] wordToChar;
    private String WORD_HIDDEN = "";
    private String currentHiddenWord;
    private boolean isLetterPresent;
    private int hangmanErrors = -1;
    private int idGame;

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
        autoInsert();
    }

    private String getWord() {
        try {
            switch (BotStartConfig.getMapGameLanguages().get(getUserId())) {
                case "rus" -> {
                    JSONObject json = new JSONObject(IOUtils.toString(new URL(URL_RU), StandardCharsets.UTF_8));
                    return String.valueOf(json.getString("word"));
                }
                case "eng" -> {
                    JSONObject json = new JSONObject(IOUtils.toString(new URL(URL_EN), StandardCharsets.UTF_8));
                    return String.valueOf(json.getString("word"));
                }
            }
        } catch (Exception e) {
            System.out.println("Скорее всего API не работает");
            e.printStackTrace();
        }
        return null;
    }

    private void updateEmbedBuilder(EmbedBuilder start) {
        Instant specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(specificTime))));

        HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId),
                String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L)));

        start.setColor(0x00FF00);
        start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

        start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                BotStartConfig.getMapPrefix().get(guildId) == null ? "!" : BotStartConfig.getMapPrefix().get(guildId)), false);

        start.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
        start.addField("Attention for Admins", "[Add slash commands](https://discord.com/api/oauth2/authorize?client_id=845974873682608129&scope=applications.commands%20bot)", false);
        start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
        start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
        start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
        start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L));
        start.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
    }

    public void startGame(@NotNull SlashCommandEvent event) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                //Добавляем buttons в коллекции
                insertButtonsToCollection();
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                event.replyEmbeds(needSetLanguage.build())
                        .addActionRow(buttons)
                        .queue();
                clearingCollections();
                return;
            }

            WORD = getWord();
            if (WORD != null) {
                wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            } else {
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", event.getUser().getId()));
                wordIsNull.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", event.getUser().getId()));

                event.replyEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            EmbedBuilder start = new EmbedBuilder();

            //Заполняем EmbedBuilder start
            updateEmbedBuilder(start);

            event.replyEmbeds(start.build())
                    .queue(m -> m.retrieveOriginal()
                            .queue(message -> {
                                        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId),
                                                message.getId());


                                        ActiveHangman activeHangman = new ActiveHangman();
                                        activeHangman.setUserIdLong(Long.valueOf(userId));
                                        activeHangman.setMessageIdLong(Long.valueOf(message.getId()));
                                        activeHangman.setChannelIdLong(channelId);
                                        activeHangman.setGuildLongId(Long.valueOf(guildId));
                                        activeHangman.setWord(WORD);
                                        activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
                                        activeHangman.setGuesses(guesses.toString());
                                        activeHangman.setHangmanErrors(hangmanErrors);

                                        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);
                                        activeHangman.setGameCreatedTime(new Timestamp(Timestamp.valueOf(now.toLocalDateTime()).getTime()));

                                        hangmanGameRepository.save(activeHangman);

                                    }
                            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopGameByTime() {
        try {
            //Добавляем кнопки когда игра завершена
            addButtonsWhenGameOver();

            EmbedBuilder info = new EmbedBuilder();
            info.setColor(0x00FF00);
            info.setTitle(jsonGameParsers.getLocale("gameOver", userId));
            info.setDescription(jsonGameParsers.getLocale("timeIsOver", userId));
            info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
            editMessageWithButtons(info, guildId, Long.parseLong(userId), channelId, buttons);

            WORD = null;
            clearingCollections();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startGame(TextChannel textChannel, String avatarUrl, String userName) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                //Добавляем buttons в коллекции
                insertButtonsToCollection();
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(userName, null, avatarUrl);
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", this.userId));

                textChannel.sendMessageEmbeds(needSetLanguage.build())
                        .setActionRow(buttons)
                        .queue();
                clearingCollections();
                return;
            }

            WORD = getWord();
            if (WORD != null) {
                wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            } else {
                EmbedBuilder wordIsNull = new EmbedBuilder();

                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", userId));
                wordIsNull.setAuthor(userName, null, avatarUrl);
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", userId));

                textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            EmbedBuilder start = new EmbedBuilder();

            //Заполняем EmbedBuilder start
            updateEmbedBuilder(start);

            textChannel.sendMessageEmbeds(start.build()).queue(m -> {
                        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), m.getId());
                        ActiveHangman activeHangman = new ActiveHangman();
                        activeHangman.setUserIdLong(Long.valueOf(userId));
                        activeHangman.setMessageIdLong(Long.valueOf(m.getId()));
                        activeHangman.setChannelIdLong(channelId);
                        activeHangman.setGuildLongId(Long.valueOf(guildId));
                        activeHangman.setWord(WORD);
                        activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
                        activeHangman.setGuesses(guesses.toString());
                        activeHangman.setHangmanErrors(hangmanErrors);

                        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);
                        activeHangman.setGameCreatedTime(new Timestamp(Timestamp.valueOf(now.toLocalDateTime()).getTime()));

                        hangmanGameRepository.save(activeHangman);
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: Возможно произойдет так что игру закончили. Удалили данные из БД и произойдет REPLACE и игра не завершится
    private void executeInsert() {
        try {
            if ((getGuesses().toString().length() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                countUsedLetters = getGuesses().toString().length();
                hangmanGameRepository.updateGame(Long.valueOf(userId), currentHiddenWord, guesses.toString(), hangmanErrors);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("По каким то причинам игры уже нет!");
        }
    }

    //Автоматически отправляет в БД данные
    public void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    executeInsert();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 5000);
    }

    public void logic(String inputs, Message messages) {
        messageList.add(messages);
        try {
            if (WORD == null) {
                addButtonsWhenGameOver();
                messages.getTextChannel()
                        .sendMessage(jsonParsers.getLocale("word_is_null", userId))
                        .setActionRow(buttons)
                        .queue();
                clearingCollections();
                return;
            }
        } catch (Exception e) {
            System.out.println("Word null");
        }

        if (WORD_HIDDEN.contains("_")) {

            boolean contains = guesses.toString().contains(inputs.toUpperCase());
            if (!contains) {
                addGuesses(inputs.toUpperCase());
            }

            isLetterPresent = contains;

            if (isLetterPresent()) {
                try {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    info.appendDescription(jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId));

                    info.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
                    info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                    info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                    info.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    info.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    editMessage(info, guildId, Long.parseLong(userId), this.channelId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            if (WORD.contains(inputs)) {
                char c = inputs.charAt(0);
                checkLetterInWord(wordToChar, c);
                String result = replacementLetters(WORD.indexOf(inputs));

                if (!result.contains("_")) {
                    try {
                        //Добавляем кнопки когда игра завершена
                        addButtonsWhenGameOver();

                        EmbedBuilder win = new EmbedBuilder();
                        win.setColor(0x00FF00);
                        win.setDescription(jsonGameParsers.getLocale("Game_Stop_Win", userId));
                        win.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
                        win.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        win.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        win.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                        editMessageWithButtons(win, guildId, Long.parseLong(userId), channelId, buttons);

                        WORD = null;
                        clearingCollections();
                        resultGame(true);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    info.setDescription(jsonGameParsers.getLocale("Game_You_Guess_Letter", userId));

                    info.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
                    info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                    info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                    info.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    info.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    editMessage(info, guildId, Long.parseLong(userId), channelId);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 6) {
                    try {
                        //Добавляем кнопки когда игра завершена
                        addButtonsWhenGameOver();

                        EmbedBuilder info = new EmbedBuilder();
                        info.setColor(0x00FF00);
                        info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                        info.setDescription(jsonGameParsers.getLocale("Game_You_Lose", userId));

                        info.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
                        info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);
                        info.addField(jsonGameParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase() + "`", false);

                        editMessageWithButtons(info, guildId, Long.parseLong(userId), channelId, buttons);

                        WORD = null;
                        clearingCollections();
                        resultGame(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        EmbedBuilder wordNotFound = new EmbedBuilder();
                        wordNotFound.setColor(0x00FF00);
                        wordNotFound.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                        wordNotFound.setDescription(jsonGameParsers.getLocale("Game_No_Such_Letter", userId));

                        wordNotFound.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                        wordNotFound.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                        wordNotFound.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));


                        editMessage(wordNotFound, guildId, Long.parseLong(userId), channelId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void resultGame(boolean resultBool) {
        try {
            idGame = HangmanRegistry.getInstance().getIdGame();

            ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);
            Timestamp timestamp = Timestamp.valueOf(now.toLocalDateTime());

            Game game = new Game();
            game.setId(idGame);
            game.setResult(resultBool);
            game.setGameDate(new Timestamp(timestamp.getTime()));

            Player player = new Player();
            player.setId(idGame);
            player.setUserIdLong(Long.valueOf(userId));
            player.setGames_id(game);

            gamesRepository.save(game);
            playerRepository.save(player);

            hangmanGameRepository.deleteActiveGame(Long.valueOf(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    private String replacementLetters(int length) {
        if (currentHiddenWord == null) {
            currentHiddenWord = WORD_HIDDEN;
        }
        StringBuilder sb = new StringBuilder(currentHiddenWord);

        for (int i = 0; i < index.size(); i++) {
            sb.replace(index.get(i) == 0 ? index.get(i) : index.get(i) * 2,
                    index.get(i) == 0 ? index.get(i) + 1 : index.get(i) * 2 + 1,
                    String.valueOf(wordToChar[length]));
        }
        index.clear();
        currentHiddenWord = sb.toString();
        return sb.toString();
    }

    //Ищет все одинаковые буквы и записывает в коллекцию
    private void checkLetterInWord(char[] checkArray, char letter) {
        for (int i = 0; i < checkArray.length; i++) {
            if (checkArray[i] == letter) {
                index.add(i);
            }
        }
    }

    public void updateVariables(String guesses, String word, String currentHiddenWord, int hangmanErrors) {
        this.guesses.append(guesses);
        this.WORD = word;
        this.WORD_HIDDEN = currentHiddenWord;
        this.currentHiddenWord = currentHiddenWord;
        this.hangmanErrors = hangmanErrors;
        this.wordToChar = word.toCharArray();
    }

    private void clearingCollections() {
        try {
            if (BotStartConfig.jda.getGuildById(guildId) != null
                    && messageList.size() > 2
                    && BotStartConfig.jda
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId), Permission.MESSAGE_MANAGE)) {
                deleteUserGameMessages(guildId, channelId, messageList);
            }

            HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
            HangmanRegistry.getInstance().getMessageId().remove(Long.parseLong(userId));
            HangmanRegistry.getInstance().getTimeCreatedGame().remove(Long.valueOf(userId));
            HangmanRegistry.getInstance().getEndAutoDelete().remove(Long.valueOf(userId));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertButtonsToCollection() {
        buttons.add(Button.secondary(ReactionsButton.BUTTON_RUS, "Кириллица")
                .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        buttons.add(Button.secondary(ReactionsButton.BUTTON_ENG, "Latin")
                .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        buttons.add(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play"));
    }

    private void addButtonsWhenGameOver() {
        if (!buttons.isEmpty()) {
            buttons.clear();
        }

        buttons.add(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"));

        if (BotStartConfig.getMapGameLanguages().get(getUserId()).equals("eng")) {
            buttons.add(Button.secondary(ReactionsButton.BUTTON_CHANGE_GAME_LANGUAGE, "Кириллица")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        } else {
            buttons.add(Button.secondary(ReactionsButton.BUTTON_CHANGE_GAME_LANGUAGE, "Latin")
                    .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        }

        buttons.add(Button.primary(ReactionsButton.BUTTON_MY_STATS, "My stats"));
    }

    private void addGuesses(String letter) {
        guesses.append(guesses.length() == 0 ? letter : ", " + letter);
    }
}