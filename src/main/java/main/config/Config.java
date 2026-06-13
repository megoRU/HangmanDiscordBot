package main.config;

import lombok.Getter;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("PROD_TOKEN");

    public static final Boolean IS_PROXY = Boolean.parseBoolean(System.getenv("IS_PROXY"));
    public static final String PROXY_IP = String.valueOf(System.getenv("PROXY_IP"));

    @Getter
    private static final String TOKEN = PRODUCTION_BOT_TOKEN;
    @Getter
    private static final String GPT_TOKEN = System.getenv("GPT_TOKEN");
    private static volatile boolean IS_DEV = true;

    static {
        if (TOKEN.equals(PRODUCTION_BOT_TOKEN)) {
            IS_DEV = false;
        }
    }

    public static boolean isIsDev() {
        return IS_DEV;
    }
}