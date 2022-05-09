package main.jsonparser;

import main.config.BotStartConfig;

public class JSONParsers {

    private final Locale locale;

    public JSONParsers(Locale locale) {
        this.locale = locale;
    }

    public String getLocale(String key, String userIdLong) {
        try {
            String language = "eng";
            if (locale.equals(Locale.GAME)) {
                if (BotStartConfig.getMapGameLanguages().get(userIdLong) != null) {
                    language = BotStartConfig.getMapGameLanguages().get(userIdLong);
                }
            } else {
                if (BotStartConfig.getMapLanguages().get(userIdLong) != null) {
                    language = BotStartConfig.getMapLanguages().get(userIdLong);
                }
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ParserClass.getInstance().getTranslation(key, "eng");
    }

    public enum Locale {
        GAME,
        BOT
    }
}