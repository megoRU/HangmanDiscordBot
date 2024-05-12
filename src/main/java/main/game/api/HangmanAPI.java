package main.game.api;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import main.model.entity.UserSettings;
import main.service.UserSettingsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HangmanAPI {

    private final MegoruAPI megoruAPI;
    private final UserSettingsService userSettingsService;

    @Autowired
    public HangmanAPI(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
        this.megoruAPI = new MegoruAPI.Builder().build();
    }

    @NotNull
    public String getWord(long userId) throws UnsuccessfulHttpException, IOException, NullPointerException {
        UserSettings.GameLanguage gameLanguage = userSettingsService.getUserGameLanguage(userId);
        UserSettings.Category category = userSettingsService.getUserCategory(userId);

        if (gameLanguage != null && category != null) {
            GameWordLanguage gameWordLanguage = new GameWordLanguage();
            gameWordLanguage.setLanguage(gameLanguage.name());
            gameWordLanguage.setCategory(category.name());

            String word = megoruAPI.getWord(gameWordLanguage).getWord();
            if (word == null || word.isEmpty()) throw new NullPointerException("Word is Null");
            return megoruAPI.getWord(gameWordLanguage).getWord();
        } else {
            throw new NullPointerException("Language or Category is Null");
        }
    }
}