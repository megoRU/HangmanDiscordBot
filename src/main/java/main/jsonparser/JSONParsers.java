package main.jsonparser;

import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JSONParsers {

    private final Locale locale;
    private final static Logger LOGGER = LoggerFactory.getLogger(JSONParsers.class.getName());

    public JSONParsers(Locale locale) {
        this.locale = locale;
    }

    public String getLocale(String key, long userIdLong) {
        try {
            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
            UserSettings userSettings = userSettingsMap.get(userIdLong);

            if (locale.equals(Locale.GAME)) {
                if (userSettings != null) {
                    return userSettings.getGameLanguage().name();
                }
            } else {
                if (userSettings != null) {
                    return userSettings.getBotLanguage().name();
                }
            }
            return ParserClass.getInstance().getTranslation(key, "EN");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return ParserClass.getInstance().getTranslation(key, "EN");
    }

    public enum Locale {
        GAME,
        BOT
    }
}