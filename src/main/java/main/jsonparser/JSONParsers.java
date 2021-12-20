package main.jsonparser;

import main.config.BotStartConfig;

public class JSONParsers {

    public String getLocale(String key, String userIdLong) {
        try {
            String language = "eng";
            if (BotStartConfig.getMapLanguages().get(userIdLong) != null) {
                language = BotStartConfig.getMapLanguages().get(userIdLong);
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}