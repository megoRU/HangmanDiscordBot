package hangman;

import db.DataBase;
import jsonparser.JSONGameParsers;
import jsonparser.JSONParsers;
import lombok.Getter;
import lombok.Setter;
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
import startbot.BotStart;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    public Hangman(String userId, String guildId, Long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        autoInsert();
    }

    private String getWord() {
        try {
            switch (BotStart.getMapGameLanguages().get(getUserId())) {
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
            e.printStackTrace();
        }
        System.out.println("Скорее всего API не работает");
        return null;
    }

    public void startGame(@NotNull SlashCommandEvent event) {
        try {
            if (BotStart.getMapGameLanguages().get(getUserId()) == null) {
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play"));

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
                event.reply(jsonParsers.getLocale("errors", userId)).queue();
                clearingCollections();
                return;
            }

            Instant specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

            HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(specificTime))));

            HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId),
                    String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L)));

            EmbedBuilder start = new EmbedBuilder();
            start.setColor(0x00FF00);
            start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

            start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                    BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId)), false);

            start.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
            start.addField("Attention for Admins", "[Add slash commands](https://discord.com/api/oauth2/authorize?client_id=845974873682608129&scope=applications.commands%20bot)", false);
            start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
            start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
            start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + hideWord(WORD.length()) + "`", false);
            start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L));
            start.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

            event.replyEmbeds(start.build())
                    .queue(m -> m.retrieveOriginal()
                            .queue(message -> {
                                        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId),
                                                message.getId());
                                        DataBase.getInstance().createGame(userId,
                                                message.getId(),
                                                String.valueOf(channelId),
                                                guildId,
                                                WORD,
                                                WORD_HIDDEN,
                                                guesses.toString(),
                                                String.valueOf(hangmanErrors)
                                        );
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
            resultGame(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startGame(TextChannel textChannel, String avatarUrl, String userName) {
        try {
            if (BotStart.getMapGameLanguages().get(getUserId()) == null) {
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play"));

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
                textChannel.sendMessage(jsonParsers.getLocale("errors", userId)).queue();
                clearingCollections();
                return;
            }

            Instant specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

            HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(specificTime))));

            HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId),
                    String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L)));

            EmbedBuilder start = new EmbedBuilder();
            start.setColor(0x00FF00);
            start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

            start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                    BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId)), false);

            start.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
            start.addField("Attention for Admins", "[Add slash commands](https://discord.com/api/oauth2/authorize?client_id=845974873682608129&scope=applications.commands%20bot)", false);
            start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
            start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
            start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + hideWord(WORD.length()) + "`", false);
            start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(10L));
            start.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

            textChannel.sendMessageEmbeds(start.build()).queue(m -> {
                        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), m.getId());
                        DataBase.getInstance().createGame(userId,
                                m.getId(),
                                String.valueOf(channelId),
                                guildId,
                                WORD,
                                WORD_HIDDEN,
                                guesses.toString(),
                                String.valueOf(hangmanErrors)
                        );
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
                DataBase.getInstance().updateGame(userId, currentHiddenWord, guesses.toString(), String.valueOf(hangmanErrors));
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
                        info.addField(jsonGameParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase().replaceAll(".(?!$)", "$0 ") + "`", false);

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

    private void addButtonsWhenGameOver() {
        buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play again"));

        if (BotStart.getMapGameLanguages().get(getUserId()).equals("eng")) {
            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        } else {
            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                    .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
        }

        buttons.add(Button.primary(guildId + ":" + ReactionsButton.MY_STATS, "My stats"));
    }

    private void addGuesses(String letter) {
        guesses.append(guesses.length() == 0 ? letter : ", " + letter);
    }

    private void resultGame(boolean resultBool) {
        try {
            idGame = HangmanRegistry.getInstance().getIdGame();
            DataBase.getInstance().addResultGame(idGame, resultBool);
            DataBase.getInstance().addResultPlayer(Long.parseLong(userId), idGame);
            DataBase.getInstance().deleteActiveGame(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearingCollections() {
        try {
            if (messageList.size() > 2 && BotStart.getShardManager()
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStart.getShardManager().getGuildById(guildId).getTextChannelById(channelId), Permission.MESSAGE_MANAGE)) {

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

    //Создает скрытую линию из длины слова
    private String hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            sb.append(sb.length() == 0 ? "_" : " _");
            i++;
        }
        return WORD_HIDDEN = sb.toString();
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

}