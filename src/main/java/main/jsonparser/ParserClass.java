package main.jsonparser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ParserClass {

    public static final ConcurrentMap<String, String> russian = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, String> english = new ConcurrentHashMap<>();
    private static volatile ParserClass parserClass;

    private ParserClass() {
    }

    public static ParserClass getInstance() {
        if (parserClass == null) {
            synchronized (ParserClass.class) {
                if (parserClass == null) {
                    parserClass = new ParserClass();
                }
            }
        }
        return parserClass;
    }

    public String getTranslation(String key, String language) {
        if (language == null || language.equals("EN")) return english.get(key);
        else return russian.get(key);
    }
}
