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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Setter
@Getter
public class Hangman implements HangmanHelper {

    private static final String URL_RU = "http://45.138.72.66:8085/api/russian";
    private static final String URL_EN = "http://45.138.72.66:8085/api/english";
    private static final String HANGMAN_URL = "https://megoru.ru/hangman2/";
    private final StringBuilder guesses = new StringBuilder();
    private final List<Integer> index = new ArrayList<>();
    private int countUsedLetters;
    private final String userId;
    private final String guildId;
    private final Long channelId;
    private static final JSONGameParsers jsonGameParsers = new JSONGameParsers();
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final List<Message> messageList = new ArrayList<>(17);
    private final List<Button> buttons = new ArrayList<>();
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

                event.reply(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId))
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
                        String.valueOf(hangmanErrors));
                    }
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGame(TextChannel channel) {
        try {
            if (BotStart.getMapGameLanguages().get(getUserId()) == null) {
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play"));

                channel.sendMessage(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId))
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
                channel.sendMessage(jsonParsers.getLocale("errors", userId)).queue();
                clearingCollections();
                return;
            }

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

            channel.sendMessageEmbeds(start.build()).queue(m -> {
                        HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), m.getId());
                        DataBase.getInstance().createGame(userId,
                                m.getId(),
                                String.valueOf(channelId),
                                guildId,
                                WORD,
                                WORD_HIDDEN,
                                guesses.toString(),
                                String.valueOf(hangmanErrors));
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


                    editMessage(info, guildId, Long.parseLong(userId), this.channelId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            if (WORD.contains(inputs)) {
                char c = inputs.charAt(0);
                checkMethod(wordToChar, c);
                String result = replacementLetters(WORD.indexOf(inputs));

                if (!result.contains("_")) {
                    try {
                        buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play again"));

                        if (BotStart.getMapGameLanguages().get(getUserId()).equals("eng")) {
                            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                        } else {
                            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                                    .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                        }

                        buttons.add(Button.primary(guildId + ":" + ReactionsButton.MY_STATS, "My stats"));

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

                    editMessage(info, guildId, Long.parseLong(userId), channelId);
                    info.clear();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 6) {
                    try {
                        buttons.add(Button.success(guildId + ":" + ReactionsButton.START_NEW_GAME, "Play again"));

                        if (BotStart.getMapGameLanguages().get(getUserId()).equals("eng")) {
                            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                        } else {
                            buttons.add(Button.secondary(guildId + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                                    .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                        }

                        buttons.add(Button.primary(guildId + ":" + ReactionsButton.MY_STATS, "My stats"));


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

                        editMessage(wordNotFound, guildId, Long.parseLong(userId), channelId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
            if (messageList.size() > 2 && BotStart.getJda().getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStart.getJda().getGuildById(guildId).getTextChannelById(channelId), Permission.MESSAGE_MANAGE)) {

                BotStart.getJda()
                        .getGuildById(guildId)
                        .getTextChannelById(channelId)
                        .deleteMessages(messageList).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        HangmanRegistry.getInstance().getMessageId().remove(Long.parseLong(userId));
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
    private void checkMethod(char[] checkArray, char letter) {
        for (int i = 0; i < checkArray.length; i++) {
            if (checkArray[i] == letter) {
                checkArray[i] = letter;
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