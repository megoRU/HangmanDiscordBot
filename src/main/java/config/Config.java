package config;

public class Config {

  private static final String DEV_BOT_TOKEN = "";
  public static final String DEV_BOT_TOKEN2 = "";
  private static final String PRODUCTION_BOT_TOKEN = "";
  private static final String TOKEN = DEV_BOT_TOKEN;
  private static final String HANGMAN_NAME = "";

  //Данный от БД с Hangman
  private static final String HANGMAN_CONNECTION = ""; //utf8mb4
  private static final String HANGMAN_USER = HANGMAN_NAME;
  private static final String HANGMAN_PASS = "";

  private static final String TOP_GG_API_TOKEN = "";
  private static final String BOT_ID = "845974873682608129"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
  private static final String STATCORD = "";
  private static final String URL = "https://discord.com/oauth2/authorize?client_id=845974873682608129&scope=bot&permissions=2147544128";

  public static String getTOKEN() {
    return TOKEN;
  }

  public static String getHangmanConnection() {
    return HANGMAN_CONNECTION;
  }

  public static String getHangmanUser() {
    return HANGMAN_USER;
  }

  public static String getHangmanPass() {
    return HANGMAN_PASS;
  }

  public static String getTopGgApiToken() {
    return TOP_GG_API_TOKEN;
  }

  public static String getBotId() {
    return BOT_ID;
  }

  public static String getStatcord() {
    return STATCORD;
  }
}