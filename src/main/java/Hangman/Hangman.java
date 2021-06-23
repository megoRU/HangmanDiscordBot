package Hangman;

import db.DataBase;
import jsonparser.JSONGameParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import startbot.BotStart;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;

public class Hangman implements HangmanHelper {

    private String WORD = null;
    private char[] strToArray;
    private String WORD_HIDDEN = "";
    private final ArrayList<String> wordList = new ArrayList<>();
    private final ArrayList<Integer> index = new ArrayList<>();
    private final ArrayList<String> usedLetters = new ArrayList<>();
    private boolean isLetterPresent;
    private Integer count = 0;
    private Integer count2 = 0;
    private final String userId;
    private final String guildId;
    private final TextChannel channel;
    private int idGame;
    private final JSONGameParsers jsonParsers = new JSONGameParsers();
    private static final String URL_RU = "http://45.138.72.66:8085/api/russian";
    private static final String URL_EN = "https://random-word-api.herokuapp.com/word?number=1";

    public Hangman(String userId, String guildId, TextChannel channel) {
        this.userId = userId;
        this.guildId = guildId;
        this.channel = channel;
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

    public void startGame(TextChannel channel, User user) {
        try {
            setIdGame();
            if (WORD == null) {
                WORD = getWord();
                strToArray = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
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
            start.setTitle(jsonParsers.getLocale("Game_Title", userId));
            start.setDescription(jsonParsers.getLocale("Game_Start", userId).replaceAll("\\{0}", BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId))
                    + getDescription(count2)
                    + jsonParsers.getLocale("Game_Current_Word", userId) + hideWord(WORD.length()) + "`"
                    + jsonParsers.getLocale("Game_Player", userId) + Long.parseLong(userId) + ">");

            channel.sendMessage(start.build()).queue(m -> HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), m.getId()));
            start.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logic(String inputs) {

        if (WORD_HIDDEN.contains("_")) {

            for (String listLoop : usedLetters) {
                if (listLoop.contains(inputs)) {
                    isLetterPresent = true;
                    break;
                }
                if (!listLoop.contains(inputs)) {
                    isLetterPresent = false;
                }
            }

            if (!isIsLetterPresent()) {
                usedLetters.add(inputs);
            }

            if (isIsLetterPresent()) {
                try {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonParsers.getLocale("Game_Title", userId));
                    info.setDescription(jsonParsers.getLocale("Game_You_Use_This_Letter", userId)
                            + jsonParsers.getLocale("Game_Have_Attempts", userId) + (6 - count2) + "`\n"
                            + getDescription(count2)
                            + jsonParsers.getLocale("Game_Current_Word", userId) + replacementLetters(WORD.indexOf(inputs)) + "`"
                            + jsonParsers.getLocale("Game_Player", userId) + userId + ">");

                    editMessage(info, Long.parseLong(userId), Long.parseLong(userId), this.channel.getIdLong());
                    info.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            if (WORD.contains(inputs)) {
                char c = inputs.charAt(0);
                checkMethod(strToArray, c);
                String result = replacementLetters(WORD.indexOf(inputs));

                if (!wordList.get(wordList.size() - 1).contains("_")) {
                    try {
                        EmbedBuilder win = new EmbedBuilder();
                        win.setColor(0x00FF00);
                        win.setTitle(jsonParsers.getLocale("Game_Title", userId));
                        win.setDescription(jsonParsers.getLocale("Game_Stop_Win", userId)
                                + getDescription(count2)
                                + jsonParsers.getLocale("Game_Current_Word", userId) + result + "`"
                                + jsonParsers.getLocale("Game_Player", userId) + Long.parseLong(userId) + ">");

                        channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), win.build())
                                .queue(message -> message.addReaction(Reactions.emojiNextTrack).queue());
                        win.clear();
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
                    info.setTitle(jsonParsers.getLocale("Game_Title", userId));
                    info.setDescription(jsonParsers.getLocale("Game_You_Guess_Letter", userId)
                            + jsonParsers.getLocale("Game_Have_Attempts", userId) + (6 - count2) + "`\n"
                            + getDescription(count2)
                            + jsonParsers.getLocale("Game_Current_Word", userId) + result + "`"
                            + jsonParsers.getLocale("Game_Player", userId) + Long.parseLong(userId) + ">");

                    channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), info.build())
                            .queue(null, (exception) -> channel.sendMessage(removeGameException(Long.parseLong(userId))).queue());
                    info.clear();

                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!WORD.contains(inputs)) {
                count2++;

                if (count2 >= 5) {
                    try {
                        EmbedBuilder info = new EmbedBuilder();
                        info.setColor(0x00FF00);
                        info.setTitle(jsonParsers.getLocale("Game_Title", userId));
                        info.setDescription(jsonParsers.getLocale("Game_You_Lose", userId)
                                + getDescription(count2)
                                + jsonParsers.getLocale("Game_Current_Word", userId) + replacementLetters(WORD.indexOf(inputs)) + "`"
                                + jsonParsers.getLocale("Game_Word_That_Was", userId) + WORD + "`"
                                + jsonParsers.getLocale("Game_Player", userId) + Long.parseLong(userId) + ">");

                        channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), info.build())
                                .queue(message -> message.addReaction(Reactions.emojiNextTrack).queue());
                        info.clear();
                        WORD = null;
                        clearingCollections();
                        resultGame(false);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (count2 < 5) {
                    try {
                        EmbedBuilder wordNotFound = new EmbedBuilder();
                        wordNotFound.setColor(0x00FF00);
                        wordNotFound.setTitle(jsonParsers.getLocale("Game_Title", userId));
                        wordNotFound.setDescription(jsonParsers.getLocale("Game_No_Such_Letter", userId)
                                + jsonParsers.getLocale("Game_Attempts_Left", userId) + (6 - count2) + "`\n"
                                + getDescription(count2)
                                + jsonParsers.getLocale("Game_Current_Word", userId) + replacementLetters(WORD.indexOf(inputs)) + "`"
                                + jsonParsers.getLocale("Game_Player", userId) + Long.parseLong(userId) + ">");

                        channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), wordNotFound.build())
                                .queue(null, (exception) ->
                                        channel.sendMessage(removeGameException(Long.parseLong(userId))).queue());
                        wordNotFound.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void resultGame(boolean resultBool) {
        DataBase.getInstance().addResultGame(idGame, resultBool, Instant.now().toEpochMilli());
        DataBase.getInstance().addResultPlayer(Long.parseLong(userId), idGame);
    }

    private String getDescription(int count) {
        return "```"
                + (count >= 0 ? "|‾‾‾‾‾‾|      " : " ")
                + "   \n|     "
                + (count >= 1 ? "🎩" : " ")
                + "   \n|     "
                + (count >= 2 ? "\uD83E\uDD75" : " ")
                + "   \n|   "
                + (count >= 3 ? "👌👕\uD83E\uDD19" : " ")
                + "   \n|     "
                + (count >= 4 ? "🩳" : " ")
                + "   \n|    "
                + (count >= 5 ? "👞👞" : " ")
                + "   \n|     \n|__________\n\n"
                + "```";
    }

    private void clearingCollections() {
        HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        HangmanRegistry.getInstance().getMessageId().remove(Long.parseLong(userId));
    }

    //Создает скрытую линию из длины слова
    private String hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append('_');
        }
        return WORD_HIDDEN = sb.toString();
    }

    //заменяет "_" на букву которая есть в слове
    private String replacementLetters(int length) {
        if (count < 1) {
            wordList.add(WORD_HIDDEN);
            count++;
        }
        int size = wordList.size() - 1;
        StringBuilder sb = new StringBuilder(wordList.get(size));
        for (int i = 0; i < index.size(); i++) {
            sb.replace(index.get(i), index.get(i) + 1, String.valueOf(strToArray[length]));
        }
        wordList.add(sb.toString());
        index.clear();
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

    private int getIdGame() {
        return idGame;
    }

    private String removeGameException(long userId) {
        HangmanRegistry.getInstance().getActiveHangman().remove(userId);
        WORD = null;
        return "Игра была отменена из-за того, что бот не смог получить ID\n" +
                "своего сообщения для редактирования. Попробуйте ещё раз.";
    }

    public String getUserId() {
        return userId;
    }

    public String getGuildId() {
        return guildId;
    }

    public TextChannel getChannel() {
        return channel;
    }

    private boolean isIsLetterPresent() {
        return isLetterPresent;
    }
}