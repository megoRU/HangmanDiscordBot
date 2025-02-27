package main.jsonparser;

import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONParsers {

    private final Locale locale;
    private final static Logger LOGGER = LoggerFactory.getLogger(JSONParsers.class.getName());

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
            LOGGER.error(e.getMessage());
        }
        return ParserClass.getInstance().getTranslation(key, "EN");
    }

    public enum Locale {
        GAME,
        BOT
    }
}