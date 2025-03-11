package main.jsonparser;

import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONParsers {

    private final Locale locale;
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONParsers.class.getName());

    public JSONParsers(Locale locale) {
        this.locale = locale;
    }

    public String getLocale(String key, long userIdLong) {
        try {
            UserSettings userSettings = BotStartConfig.userSettingsMap.get(userIdLong);
            if (userSettings == null) {
                return ParserClass.getInstance().getTranslation(key, "EN");
            }

            String name = switch (locale) {
                case GAME -> userSettings.getGameLanguage().name();
                case BOT -> userSettings.getBotLanguage().name();
            };

            if (name.equals("RU")) {
                return ParserClass.getInstance().getTranslation(key, "RU");
            } else {
                return ParserClass.getInstance().getTranslation(key, "EN");
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching locale translation: {}", e.getMessage(), e);
        }
        return ParserClass.getInstance().getTranslation(key, "EN");
    }

    public enum Locale {
        GAME,
        BOT
    }
}