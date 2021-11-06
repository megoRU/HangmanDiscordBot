package config;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("PROD_TOKEN");
    private static final String TOKEN = PRODUCTION_BOT_TOKEN;
    private static final String HANGMAN_NAME = "DiscordBotHangman";

    //Данный от БД с Hangman
    private static final String HANGMAN_CONNECTION = System.getenv("DATABASE_URL") + HANGMAN_NAME + System.getenv("DATABASE_URL2"); //utf8mb4
    private static final String HANGMAN_USER = HANGMAN_NAME;
    private static final String HANGMAN_PASS = System.getenv("DATABASE_PASS");

    private static final String TOP_GG_API_TOKEN = System.getenv("TOP_GG_API_TOKEN");
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