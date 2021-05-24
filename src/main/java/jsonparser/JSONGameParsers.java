package jsonparser;

import startbot.BotStart;

public class JSONGameParsers {

    public String getLocale(String key, String userIdLong) {
        try {
            String language = "eng";
            if (BotStart.getMapGameLanguages().get(userIdLong) != null) {
                language = BotStart.getMapGameLanguages().get(userIdLong);
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}