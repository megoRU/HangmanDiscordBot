package jsonparser;

import Hangman.HangmanRegistry;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ParserClass {

    private static volatile ParserClass parserClass;

    public static ParserClass getInstance() {
        if (parserClass == null) {
            synchronized (HangmanRegistry.class) {
                if (parserClass == null) {
                    parserClass = new ParserClass();
                }
            }
        }
        return parserClass;
    }

    private ParserClass() {}

    public String getTranslation(String key, String language) {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/json/" + language + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            JSONObject jsonObject = (JSONObject) obj;
            reader.close();
            inputStream.close();
            reader.close();
            return jsonObject.get(key).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}
