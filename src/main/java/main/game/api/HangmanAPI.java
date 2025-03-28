package main.game.api;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class HangmanAPI {

    private final MegoruAPI megoruAPI;

    public HangmanAPI() {
        this.megoruAPI = new MegoruAPI.Builder().build();
    }

    //TODO: NPE Language
    @NotNull
    public String getWord(long userId) throws UnsuccessfulHttpException, IOException, NullPointerException {
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userId);

        UserSettings.GameLanguage gameLanguage = userSettings.getGameLanguage();
        UserSettings.Category category = userSettings.getCategory();

        GameWordLanguage gameWordLanguage = new GameWordLanguage();
        gameWordLanguage.setLanguage(gameLanguage.name());
        gameWordLanguage.setCategory(category.name());

        String word = megoruAPI.getWord(gameWordLanguage).getWord();
        if (word == null || word.isEmpty()) throw new NullPointerException("Word is Null");
        return megoruAPI.getWord(gameWordLanguage).getWord();
    }
}