package hangman;

import db.DataBase;
import jsonparser.JSONGameParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import startbot.BotStart;

import java.awt.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Hangman implements HangmanHelper {

    private static final String URL_RU = "http://45.138.72.66:8085/api/russian";
    private static final String URL_EN = "https://random-word-api.herokuapp.com/word?number=1";
    private final StringBuilder guesses = new StringBuilder();
    private final ArrayList<String> wordList = new ArrayList<>();
    private final ArrayList<Integer> index = new ArrayList<>();
    private final ArrayList<String> usedLetters = new ArrayList<>();
    private final String userId;
    private final String guildId;
    private final TextChannel channel;
    private final JSONGameParsers jsonParsers = new JSONGameParsers();
    private final List<Message> messageList = new ArrayList<>(17);
    private final List<Button> buttons = new ArrayList<>();
    private String WORD = null;
    private char[] strToArray;
    private String WORD_HIDDEN = "";
    private boolean isLetterPresent;
    private Integer count = 0;
    private Integer count2 = 0;
    private int idGame;

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

    public void startGame(TextChannel channel) {
        try {
            if (BotStart.getMapGameLanguages().get(getUserId()) == null) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.RED);
                embedBuilder.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userId)
                        .replaceAll("\\{0}", BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId)));

                channel.sendMessageEmbeds(embedBuilder.build()).queue();
                clearingCollections();
                return;
            }

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

            start.addField(jsonParsers.getLocale("Game_Start_How_Play", userId), jsonParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                    BotStart.getMapPrefix().get(guildId) == null ? "!" : BotStart.getMapPrefix().get(guildId)), false);

            start.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
            start.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
            start.addField(jsonParsers.getLocale("Game_Guesses", userId), "", false);
            start.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + hideWord(WORD.length()) + "`", false);


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
                    info.appendDescription(jsonParsers.getLocale("Game_You_Use_This_Letter", userId));

                    info.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
                    info.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                    info.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);


                    editMessage(info, Long.parseLong(guildId), Long.parseLong(userId), this.channel.getIdLong());
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

                if (!result.contains("_")) {
                    try {
                        buttons.add(Button.success(userId + ":" + ReactionsButton.START_NEW_GAME, "Play again"));
                        buttons.add(Button.primary(userId + ":" + ReactionsButton.START_CHANGE_GAME_LANGUAGE, jsonParsers.getLocale("Hangman_Change_Language", userId)
                                + (BotStart.getMapGameLanguages().get(getUserId()).equals("rus") ? "eng" : "rus")));
                        buttons.add(Button.primary(userId + ":" + ReactionsButton.MY_STATS, "My stats"));

                        EmbedBuilder win = new EmbedBuilder();
                        win.setColor(0x00FF00);
                        win.setDescription(jsonParsers.getLocale("Game_Stop_Win", userId));
                        win.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
                        win.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        win.addField(jsonParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        win.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                        channel.editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), win.build())
                                .setActionRow(buttons)
                                .queue();

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
                    info.setDescription(jsonParsers.getLocale("Game_You_Guess_Letter", userId));

                    info.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
                    info.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                    info.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                    channel.editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), info.build())
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
                        buttons.add(Button.success(userId + ":" + ReactionsButton.START_NEW_GAME, "Play again"));
                        buttons.add(Button.primary(userId + ":" + ReactionsButton.START_CHANGE_GAME_LANGUAGE, jsonParsers.getLocale("Hangman_Change_Language", userId)
                                + (BotStart.getMapGameLanguages().get(getUserId()).equals("rus") ? "eng" : "rus")));
                        buttons.add(Button.primary(userId + ":" + ReactionsButton.MY_STATS, "My stats"));


                        EmbedBuilder info = new EmbedBuilder();
                        info.setColor(0x00FF00);
                        info.setTitle(jsonParsers.getLocale("Game_Title", userId));
                        info.setDescription(jsonParsers.getLocale("Game_You_Lose", userId));

                        info.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
                        info.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        info.addField(jsonParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        info.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);
                        info.addField(jsonParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase() + "`", false);

                        channel.editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), info.build())
                                .setActionRow(buttons)
                                .queue();

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
                        wordNotFound.setDescription(jsonParsers.getLocale("Game_No_Such_Letter", userId));

                        wordNotFound.setThumbnail("https://megoru.ru/hangman/" + count2 + ".png");
                        wordNotFound.addField(jsonParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        wordNotFound.addField(jsonParsers.getLocale("Game_Guesses", userId), "`" + guesses + "`", false);
                        wordNotFound.addField(jsonParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);


                        channel.editMessageEmbedsById(HangmanRegistry.getInstance().getMessageId().get(Long.parseLong(userId)), wordNotFound.build())
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

    private void addGuesses(String letter) {
        guesses.append(guesses.length() == 0 ? letter : ", " + letter);
    }

    private void resultGame(boolean resultBool) {
        DataBase.getInstance().addResultGame(idGame, resultBool);
        DataBase.getInstance().addResultPlayer(Long.parseLong(userId), idGame);

    }

    private void clearingCollections() {
        try {
            if (messageList.size() > 2 && BotStart.getJda().getGuildById(guildId).getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                BotStart.getJda()
                        .getGuildById(guildId)
                        .getTextChannelById(channel.getId())
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
        int localLength = 0;
        while (localLength < length) {
            sb.append(sb.length() == 0 ? "_" : " _");
            localLength++;
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
            sb.replace(index.get(i) == 0 ? index.get(i) : index.get(i) * 2,
                    index.get(i) == 0 ? index.get(i) + 1 : index.get(i) * 2 + 1,
                    String.valueOf(strToArray[length]));
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