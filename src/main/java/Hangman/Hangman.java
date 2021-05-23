package Hangman;

import db.DataBase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
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

  public Hangman(Guild guild, TextChannel channel, User user) {
    this.guild = guild;
    this.channel = channel;
    this.user = user;
  }

  private String getWord() throws IOException {
    final String URL = "https://evilcoder.ru/random_word/";
    Document doc = Jsoup.connect(URL)
          .userAgent(
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.72 Safari/537.36")
          .referrer("https://www.yandex.com/")
          .get();
    return doc.select("body").text().substring(3, doc.text().indexOf("П"));
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
    start.setTitle("Виселица");
    start.setDescription("Игра началась!\n"
        + "Отправляйте по одной букве в чат\n **без**" + " `!`\n"
        + getDescription(count2)
        + "\nТекущее слово: `" + hideWord(WORD.length()) + "`"
        + "\nИгрок: <@" + user.getIdLong() + ">");

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
        info.setTitle("Виселица");
        info.setDescription("Вы уже использовали эту букву!\n"
            + "У вас попыток: `" + (6 - count2) + "`\n"
            + getDescription(count2)
            + "\nТекущее слово: `" + replacementLetters(WORD.indexOf(inputs)) + "`"
            + "\nИгрок: <@" + user.getIdLong() + ">");

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
          win.setTitle("Виселица");
          win.setDescription("Игра завершена, вы победили!\n"
              + getDescription(count2)
              + "\nТекущее слово: `" + result + "`"
              + "\nИгрок: <@" + user.getIdLong() + ">");

          editMessage(win, this.guild.getIdLong(), this.user.getIdLong(), this.channel.getIdLong());
          win.clear();
          WORD = null;
          clearingCollections();
          return;
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0x00FF00);
        info.setTitle("Виселица");
        info.setDescription("Вы угадали букву!\n"
            + "У вас попыток: `" + (6 - count2) + "`\n"
            + getDescription(count2)
            + "\nТекущее слово: `" + result + "`"
            + "\nИгрок: <@" + user.getIdLong() + ">");

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
          info.setTitle("Виселица");
          info.setDescription("Вы проиграли!\n"
              + getDescription(count2)
              + "\nТекущее слово: `" + replacementLetters(WORD.indexOf(inputs)) + "`"
              + "\nСлово которое было: `" + WORD + "`"
              + "\nИгрок: <@" + user.getIdLong() + ">");

          editMessage(info, this.guild.getIdLong(), this.user.getIdLong(), this.channel.getIdLong());
          info.clear();
          WORD = null;
          clearingCollections();
          return;
        }

        if (count2 < 5) {

          EmbedBuilder wordNotFound = new EmbedBuilder();
          wordNotFound.setColor(0x00FF00);
          wordNotFound.setTitle("Виселица");
          wordNotFound.setDescription("Такой буквы нет!\n"
              + "Осталось попыток: `" + (6 - count2) + "`\n"
              + getDescription(count2)
              + "\nТекущее слово: `" + replacementLetters(WORD.indexOf(inputs)) + "`"
              + "\nИгрок: <@" + user.getIdLong() + ">");

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