package Hangman;

import jsonparser.JSONGameParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import startbot.BotStart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
  private final User user;
  private final Guild guild;
  private final TextChannel channel;
  private int idGame;
  private final JSONGameParsers jsonParsers = new JSONGameParsers();

  public Hangman(Guild guild, TextChannel channel, User user) {
    this.guild = guild;
    this.channel = channel;
    this.user = user;
  }

  private String getWord() throws IOException {
    final String URL_RU = "https://evilcoder.ru/random_word/";
    final String URL_EN = "https://random-word-api.herokuapp.com/word?number=1";

    if (BotStart.getMapGameLanguages().get(user.getId()) != null) {

      switch (BotStart.getMapGameLanguages().get(user.getId())) {
        case "rus" -> {
          Document doc = Jsoup.connect(URL_RU)
                  .userAgent(
                          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.72 Safari/537.36")
                  .referrer("https://www.yandex.com/")
                  .get();
          return doc.select("body").text().substring(3, doc.text().indexOf("П"));
        }
        case "eng" -> {
          return IOUtils.toString(new URL(URL_EN), StandardCharsets.UTF_8).replaceAll("\\[\"", "").replaceAll("\"]", "");
        }
      }
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
    } catch (Exception e) {
      e.printStackTrace();
    }

    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
    start.setDescription(jsonParsers.getLocale("Game_Start", user.getId()).replaceAll("\\{0}", BotStart.getMapPrefix().get(guild.getId()) == null ? "!" : BotStart.getMapPrefix().get(guild.getId()))
        + getDescription(count2)
        + jsonParsers.getLocale("Game_Current_Word", user.getId()) + hideWord(WORD.length()) + "`"
        + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

    channel.sendMessage(start.build()).queue(m -> HangmanRegistry.getInstance().getMessageId().put(user.getIdLong(), m.getId()));
    start.clear();
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
        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0x00FF00);
        info.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
        info.setDescription(jsonParsers.getLocale("Game_You_Use_This_Letter", user.getId())
            + jsonParsers.getLocale("Game_Have_Attempts", user.getId()) + (6 - count2) + "`\n"
            + getDescription(count2)
            + jsonParsers.getLocale("Game_Current_Word", user.getId()) + replacementLetters(WORD.indexOf(inputs)) + "`"
            + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

        editMessage(info, this.guild.getIdLong(), this.user.getIdLong(), this.channel.getIdLong());
        info.clear();
        return;
      }

      if (WORD.contains(inputs)) {
        char c = inputs.charAt(0);
        checkMethod(strToArray, c);
        String result = replacementLetters(WORD.indexOf(inputs));

        if (!wordList.get(wordList.size() - 1).contains("_")) {
          EmbedBuilder win = new EmbedBuilder();
          win.setColor(0x00FF00);
          win.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
          win.setDescription(jsonParsers.getLocale("Game_Stop_Win", user.getId())
              + getDescription(count2)
              + jsonParsers.getLocale("Game_Current_Word", user.getId()) + result + "`"
              + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

          editMessage(win, this.guild.getIdLong(), this.user.getIdLong(), this.channel.getIdLong());
          win.clear();
          WORD = null;
          clearingCollections();
          return;
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0x00FF00);
        info.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
        info.setDescription(jsonParsers.getLocale("Game_You_Guess_Letter", user.getId())
            + jsonParsers.getLocale("Game_Have_Attempts", user.getId()) + (6 - count2) + "`\n"
            + getDescription(count2)
            + jsonParsers.getLocale("Game_Current_Word", user.getId()) + result + "`"
            + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

        channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(user.getIdLong()), info.build())
            .queue(null, (exception) -> channel.sendMessage(removeGameException(user.getIdLong())).queue());
        info.clear();

        return;
      }

      if (!WORD.contains(inputs)) {
        count2++;

        if (count2 >= 5) {
          EmbedBuilder info = new EmbedBuilder();
          info.setColor(0x00FF00);
          info.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
          info.setDescription(jsonParsers.getLocale("Game_You_Lose", user.getId())
              + getDescription(count2)
              + jsonParsers.getLocale("Game_Current_Word", user.getId()) + replacementLetters(WORD.indexOf(inputs)) + "`"
              + jsonParsers.getLocale("Game_Word_That_Was", user.getId()) + WORD + "`"
              + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

          editMessage(info, this.guild.getIdLong(), this.user.getIdLong(), this.channel.getIdLong());
          info.clear();
          WORD = null;
          clearingCollections();
          return;
        }

        if (count2 < 5) {

          EmbedBuilder wordNotFound = new EmbedBuilder();
          wordNotFound.setColor(0x00FF00);
          wordNotFound.setTitle(jsonParsers.getLocale("Game_Title", user.getId()));
          wordNotFound.setDescription(jsonParsers.getLocale("Game_No_Such_Letter", user.getId())
              + jsonParsers.getLocale("Game_Attempts_Left", user.getId()) + (6 - count2) + "`\n"
              + getDescription(count2)
              + jsonParsers.getLocale("Game_Current_Word", user.getId()) + replacementLetters(WORD.indexOf(inputs)) + "`"
              + jsonParsers.getLocale("Game_Player", user.getId()) + user.getIdLong() + ">");

          channel.editMessageById(HangmanRegistry.getInstance().getMessageId().get(user.getIdLong()), wordNotFound.build())
              .queue(null, (exception) ->
                  channel.sendMessage(removeGameException(user.getIdLong())).queue());
          wordNotFound.clear();
        }
      }

    }
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
    HangmanRegistry.getInstance().removeHangman(user.getIdLong());
    HangmanRegistry.getInstance().getMessageId().remove(user.getIdLong());
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

  public User getUser() {
    return user;
  }

  public Guild getGuild() {
    return guild;
  }

  public TextChannel getChannel() {
    return channel;
  }

  private boolean isIsLetterPresent() {
    return isLetterPresent;
  }
}