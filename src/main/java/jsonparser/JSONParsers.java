package jsonparser;

import startbot.BotStart;

public class JSONParsers {

    public String getLocale(String key, String userIdLong) {
        try {
            String language = "eng";
            if (BotStart.getMapLanguages().get(userIdLong) != null) {
                language = BotStart.getMapLanguages().get(userIdLong);
            }
            return ParserClass.getInstance().getTranslation(key, language);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}