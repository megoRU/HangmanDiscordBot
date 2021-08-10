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
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import startbot.BotStart;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Hangman implements HangmanHelper {

    private static final String URL_RU = "http://45.138.72.66:8085/api/russian";
    private static final String URL_EN = "https://random-word-api.herokuapp.com/word?number=1";
    private static final String HANGMAN_URL = "https://megoru.ru/hangman2/";
    private final StringBuilder guesses = new StringBuilder(); //
    private final ArrayList<Integer> index = new ArrayList<>();
    private final StringBuilder usedLetters = new StringBuilder(); //
    private final String userId;
    private final Long guildId;
    private final Long channelId;
    private final JSONGameParsers jsonGameParsers = new JSONGameParsers();
    private final JSONParsers jsonParsers = new JSONParsers();
    private final List<Message> messageList = new ArrayList<>(17);
    private final List<Button> buttons = new ArrayList<>();
    private String WORD = null; //
    private char[] wordToChar;
    private String WORD_HIDDEN = ""; //
    private String currentHiddenWord;
    private volatile boolean isLetterPresent;
    private Integer hangmanErrors = -1; //
    private int idGame; //запрашивать у класса а то будет перезатирание

    public Hangman(String userId, Long guildId, Long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    private String getWord() {
        try {
            switch (BotStart.getMapGameLanguages().get(getUserId())) {
                case "rus" -> {
                    JSONObject json = new JSONObject(IOUtils.toString(new URL(URL_RU), StandardCharsets.UTF_8));
                    return String.valueOf(json.get("russian_WORD"));
                }
                case "eng" -> {
                    return IOUtils.toString(new URL(URL_EN), StandardCharsets.UTF_8).replaceAll("\\[\"", "").replaceAll("\"]", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "URL_RU";
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

            setIdGame();
            if (WORD == null) {
                WORD = getWord();
                wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            }

            if (WORD.equals("URL_RU")) {
                channel.sendMessage("An error occurred. The game was deleted.\nPlease try again in 5 seconds!").queue();
                WORD = null;
                clearingCollections();
                return;
            }

            EmbedBuilder start = new EmbedBuilder();
            start.setColor(0x00FF00);
            start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

            start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                    BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId)), false);

            start.setThumbnail(HANGMAN_URL + hangmanErrors + ".png");
            start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
            start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
            start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + hideWord(WORD.length()) + "`", false);


            channel.sendMessageEmbeds(start.build()).queue(m -> HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), m.getId()));
            start.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logic(String inputs, Message messages) {
        messageList.add(messages);
        if (WORD_HIDDEN.contains("_")) {

            if (!guesses.toString().contains(inputs.toUpperCase())) {
                addGuesses(inputs.toUpperCase());
            }

            isLetterPresent = usedLetters.toString().contains(inputs);

            if (!isLetterPresent()) {
                usedLetters.append(inputs);
            }

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
        DataBase.getInstance().addResultGame(idGame, resultBool);
        DataBase.getInstance().addResultPlayer(Long.parseLong(userId), idGame);
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

    private void setIdGame() {
        idGame = HangmanRegistry.getInstance().getIdGame();
    }

}