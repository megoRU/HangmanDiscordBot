package jsonparser;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import startbot.BotStart;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JSONGameParsers {

  public String getLocale(String key, String guildIdLong) {
    try {
      String language = "eng";
      if (BotStart.getMapGameLanguages().get(guildIdLong) != null) {
        language = BotStart.getMapGameLanguages().get(guildIdLong);
      }
      InputStream inputStream = getClass().getResourceAsStream("/json/" + language + ".json");
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      JSONParser parser = new JSONParser();
      Object obj = parser.parse(reader);
      JSONObject jsonObject = (JSONObject) obj;
      return jsonObject.get(key).toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "NO_FOUND_LOCALIZATION";
  }
}