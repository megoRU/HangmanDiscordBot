package main.game.api;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HangmanAPI {

    private final MegoruAPI megoruAPI;

    public HangmanAPI() {
        this.megoruAPI = new MegoruAPI.Builder().build();
    }

    @NotNull
    public String getWord(long userId) throws UnsuccessfulHttpException, IOException, NullPointerException {
        UserSettings.GameLanguage gameLanguage = BotStartConfig.getMapGameLanguages().get(userId);
        UserSettings.Category category = BotStartConfig.getMapGameCategory().get(userId);

        GameWordLanguage gameWordLanguage = new GameWordLanguage();
        gameWordLanguage.setLanguage(gameLanguage.name());
        gameWordLanguage.setCategory(category.name());

        String word = megoruAPI.getWord(gameWordLanguage).getWord();
        if (word == null || word.isEmpty()) throw new NullPointerException("Word is Null");
        return megoruAPI.getWord(gameWordLanguage).getWord();
    }
}