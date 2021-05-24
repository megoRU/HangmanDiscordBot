package startbot;

import Hangman.GameHangmanListener;
import Hangman.HangmanRegistry;
import config.Config;
import db.DataBase;
import events.MessageWhenBotJoinToGuild;
import messagesevents.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class BotStart {

  public static final String activity = "!help";
  public static final String version = "v15";
  private static JDA jda;
  private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
  private static final Map<String, String> mapPrefix = new HashMap<>();
  private static final Map<String, String> mapLanguages = new HashMap<>();
  private static final Map<String, String> mapGameLanguages = new HashMap<>();

  public void startBot() throws Exception {
    //Теперь HangmanRegistry знает колличство игр и может отдавать правильное значение
    HangmanRegistry.getInstance().getSetIdGame();
    //Получаем все префиксы из базы данных
    getPrefixFromDB();
    //Получаем все языки перевода
    getLocalizationFromDB();
    //Получаем все языки перевода для игры
    getGameLocalizationFromDB();


    jdaBuilder.setAutoReconnect(true);
    jdaBuilder.setStatus(OnlineStatus.ONLINE); //version + TopGGAndStatcordThread.serverCount +
    jdaBuilder.setActivity(Activity.playing(activity + " | " +  "!hg"));
    jdaBuilder.setBulkDeleteSplittingEnabled(false);
    jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
    jdaBuilder.addEventListeners(new PrefixChange());
    jdaBuilder.addEventListeners(new MessageInfoHelp());
    jdaBuilder.addEventListeners(new LanguageChange());
    jdaBuilder.addEventListeners(new GameLanguageChange());
    jdaBuilder.addEventListeners(new GameHangmanListener());
    jdaBuilder.addEventListeners(new MessageStats());


    jda = jdaBuilder.build();
    jda.awaitReady();

  }

  private void getPrefixFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }
      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getLocalizationFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from language";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
      }

      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getGameLocalizationFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from game_language";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapGameLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
      }

      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, String> getMapPrefix() {
    return mapPrefix;
  }

  public static Map<String, String> getMapLanguages() {
    return mapLanguages;
  }

  public static Map<String, String> getMapGameLanguages() {
    return mapGameLanguages;
  }

  public static JDA getJda() {
    return jda;
  }

}