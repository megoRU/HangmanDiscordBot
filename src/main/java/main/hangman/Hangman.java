package main.hangman;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.enums.Buttons;
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
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Queue;
import java.util.*;

@Setter
@Getter
public class Hangman implements HangmanHelper {

    private static final String URL = "http://195.2.81.139:8085/api/word";
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
    private final List<Button> buttons = new ArrayList<>();
    private final Queue<Message> messageList = new ArrayDeque<>();
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
    }

    private String getWord() {
        try {
            long time = System.currentTimeMillis();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .POST(HttpRequest.BodyPublishers.ofString(new language(BotStartConfig.getMapGameLanguages().get(getUserId())).toString()))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(System.currentTimeMillis() - time + " ms getWord()");

            return response.body();
        } catch (Exception e) {
            System.out.println("Скорее всего API не работает");
            e.printStackTrace();
        }
        return null;
    }

    //TODO: Работает, но изменить время на Instant желательно.
    private void updateEmbedBuilder(EmbedBuilder start) {
        autoInsert();

        Instant instant = Instant.now().plusSeconds(600L);

        HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(Instant.now()))));

        HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId), String.valueOf(OffsetDateTime.parse(String.valueOf(instant))));

        start.setColor(0x00FF00);
        start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

        start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                BotStartConfig.getMapPrefix().get(guildId) == null ? "!" : BotStartConfig.getMapPrefix().get(guildId)), false);

        start.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
        start.addField("Attention for Admins", "[Add slash commands](https://discord.com/api/oauth2/authorize?client_id=845974873682608129&scope=applications.commands%20bot)", false);
        start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
        start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
        start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
        start.setTimestamp(instant);
        start.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
    }

    public void startGame(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                //Добавляем buttons в коллекции
                insertButtonsToCollection();
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                event.replyEmbeds(needSetLanguage.build()).addActionRow(buttons).queue();
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

            event.replyEmbeds(start.build()).queue(m -> m.retrieveOriginal().queue(this::createEntityInDataBase));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createEntityInDataBase(Message message) {
        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), message.getId());

        ActiveHangman activeHangman = new ActiveHangman();
        activeHangman.setUserIdLong(Long.valueOf(userId));
        activeHangman.setMessageIdLong(Long.valueOf(message.getId()));
        activeHangman.setChannelIdLong(channelId);
        activeHangman.setGuildLongId(guildId != null ? Long.valueOf(guildId) : null);
        activeHangman.setWord(WORD);
        activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
        activeHangman.setGuesses(guesses.toString());
        activeHangman.setHangmanErrors(hangmanErrors);
        activeHangman.setGameCreatedTime(new Timestamp(Instant.now().toEpochMilli()));
        hangmanGameRepository.save(activeHangman);
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

            if (guildId != null) {
                editMessageWithButtons(info, guildId, Long.parseLong(userId), channelId, buttons);
            } else {
                editMessageWithButtons(info, Long.parseLong(userId), channelId, buttons);
            }
            WORD = null;
            clearingCollections();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                //Добавляем buttons в коллекции
                insertButtonsToCollection();
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(userName, null, avatarUrl);
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", this.userId));

                textChannel.sendMessageEmbeds(needSetLanguage.build()).setActionRow(buttons).queue();
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

            textChannel.sendMessageEmbeds(start.build()).queue(this::createEntityInDataBase);
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
                    if (HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                        executeInsert();
                        deleteMessages();
                    } else {
                        deleteMessages();
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    deleteMessages();
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }, 7000, 5000);
    }

    private void deleteMessages() {
        if (guildId != null) {
            if (BotStartConfig.jda
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.jda.getTextChannelById(channelId), Permission.MESSAGE_MANAGE) && !messageList.isEmpty()) {
                if (messageList.size() == 1) {
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessageById(messageList.poll().getId()).queue();
                } else {
                    List<Message> temp = new ArrayList<>(messageList);
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessages(messageList).queue();
                    messageList.removeAll(temp);
                }
            }
        }
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

                    if (guildId != null) {
                        editMessage(info, guildId, Long.parseLong(userId), this.channelId);
                    } else {
                        editMessage(info, Long.parseLong(userId), this.channelId);
                    }
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

                        if (guildId != null) {
                            editMessageWithButtons(win, guildId, Long.parseLong(userId), channelId, buttons);
                        } else {
                            editMessageWithButtons(win, Long.parseLong(userId), channelId, buttons);
                        }

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

                    if (guildId != null) {
                        editMessage(info, guildId, Long.parseLong(userId), this.channelId);
                    } else {
                        editMessage(info, Long.parseLong(userId), this.channelId);
                    }

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

                        if (guildId != null) {
                            editMessageWithButtons(info, guildId, Long.parseLong(userId), channelId, buttons);
                        } else {
                            editMessageWithButtons(info, Long.parseLong(userId), channelId, buttons);
                        }

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

                        if (guildId != null) {
                            editMessage(wordNotFound, guildId, Long.parseLong(userId), this.channelId);
                        } else {
                            editMessage(wordNotFound, Long.parseLong(userId), this.channelId);
                        }
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

            Game game = new Game();
            game.setId(idGame);
            game.setResult(resultBool);
            game.setGameDate(new Timestamp(Instant.now().toEpochMilli()));

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
            HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertButtonsToCollection() {
        if (!buttons.isEmpty()) {
            buttons.clear();
        }

        buttons.add(Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица")
                .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        buttons.add(Button.secondary(Buttons.BUTTON_ENG.name(), "Latin")
                .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        buttons.add(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"));

    }

    private void addButtonsWhenGameOver() {
        if (!buttons.isEmpty()) {
            buttons.clear();
        }

        buttons.add(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"));

        if (BotStartConfig.getMapGameLanguages().get(getUserId()).equals("eng")) {
            buttons.add(Button.secondary(Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name(), "Кириллица")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        } else {
            buttons.add(Button.secondary(Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name(), "Latin")
                    .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        }

        buttons.add(Button.primary(Buttons.BUTTON_MY_STATS.name(), "My stats"));
    }

    private void addGuesses(String letter) {
        guesses.append(guesses.length() == 0 ? letter : ", " + letter);
    }
}