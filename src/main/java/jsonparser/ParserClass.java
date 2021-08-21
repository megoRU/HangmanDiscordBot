package jsonparser;

import hangman.HangmanRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ParserClass {

    private static volatile ParserClass parserClass;
    public static final ConcurrentMap<String, String> russian = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, String> english = new ConcurrentHashMap<>();

    private ParserClass() {
    }

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

    public String getTranslation(String key, String language) {
        try {
            if (language.equals("eng")) {
                return english.get(key);
            } else {
                return russian.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}
