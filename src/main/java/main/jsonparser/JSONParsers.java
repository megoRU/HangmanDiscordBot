package main.jsonparser;

import main.config.BotStartConfig;
import main.model.entity.UserSettings;

public class JSONParsers {

    private final Locale locale;

    public JSONParsers(Locale locale) {
        this.locale = locale;
    }

    public String getLocale(String key, long userIdLong) {
        try {
            String language = UserSettings.GameLanguage.EN.name();
            if (locale.equals(Locale.GAME)) {
                if (BotStartConfig.getMapGameLanguages().get(userIdLong) != null) {
                    language = BotStartConfig.getMapGameLanguages().get(userIdLong).name();
                }
            } else {
                if (BotStartConfig.getMapLanguages().get(userIdLong) != null) {
                    language = BotStartConfig.getMapLanguages().get(userIdLong).name();
                }
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ParserClass.getInstance().getTranslation(key, "EN");
    }

    public enum Locale {
        GAME,
        BOT
    }
}