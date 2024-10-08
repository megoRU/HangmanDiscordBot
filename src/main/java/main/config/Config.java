package main.config;

import lombok.Getter;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("PROD_TOKEN");
    @Getter
    private static final String TOKEN = PRODUCTION_BOT_TOKEN;
    @Getter
    private static final String GPT_TOKEN = System.getenv("GPT_TOKEN");
    private static final String TOP_GG_API_TOKEN = System.getenv("TOP_GG_API_TOKEN");
    private static final String BOT_ID = "845974873682608129"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
    private static final String URL = "https://discord.com/oauth2/authorize?client_id=845974873682608129&permissions=2147544128&scope=applications.commands%20bot";
    private static volatile boolean IS_DEV = true;

    static {
        if (TOKEN.equals(PRODUCTION_BOT_TOKEN)) {
            IS_DEV = false;
        }
    }

    public static String getTopGgApiToken() {
        return TOP_GG_API_TOKEN;
    }

    public static String getBotId() {
        return BOT_ID;
    }

    public static boolean isIsDev() {
        return IS_DEV;
    }
}