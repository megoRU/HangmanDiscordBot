package main.jsonparser;

import main.config.BotStartConfig;

public class JSONGameParsers {

    public String getLocale(String key, String userIdLong) {
        try {
            String language = "eng";
            if (BotStartConfig.getMapGameLanguages().get(userIdLong) != null) {
                language = BotStartConfig.getMapGameLanguages().get(userIdLong);
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ParserClass.getInstance().getTranslation(key, "eng");
    }
}