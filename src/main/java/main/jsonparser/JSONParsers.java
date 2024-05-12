package main.jsonparser;

import main.model.entity.UserSettings;
import main.service.UserSettingsService;

import java.util.logging.Logger;

public class JSONParsers {

    private final Locale locale;
    private static final Logger LOGGER = Logger.getLogger(JSONParsers.class.getName());

    public JSONParsers(Locale locale) {
        this.locale = locale;
    }

    public String getLocale(String key, long userIdLong) {
        try {
            String language = UserSettings.GameLanguage.EN.name();
            UserSettings.GameLanguage gameLanguageLocal = UserSettingsService.getGameLanguage(userIdLong);
            UserSettings.GameLanguage userLanguage = UserSettingsService.getGameLanguage(userIdLong);
            if (locale.equals(Locale.GAME)) {
                if (gameLanguageLocal != null) {
                    language = gameLanguageLocal.name();
                }
            } else {
                if (userLanguage != null) {
                    language = userLanguage.name();
                }
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return ParserClass.getInstance().getTranslation(key, "EN");
    }

    public enum Locale {
        GAME,
        BOT
    }
}