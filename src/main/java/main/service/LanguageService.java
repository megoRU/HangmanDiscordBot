package main.service;

import main.jsonparser.ParserClass;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class LanguageService {

    private final static Logger LOGGER = LoggerFactory.getLogger(LanguageService.class.getName());

    public void language() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = new JSONObject(new JSONTokener(reader));

                for (String o : jsonObject.keySet()) {
                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(o, String.valueOf(jsonObject.get(o)));
                    } else {
                        ParserClass.english.put(o, String.valueOf(jsonObject.get(o)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}