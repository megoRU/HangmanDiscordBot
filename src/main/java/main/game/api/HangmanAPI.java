package main.game.api;

import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import main.model.entity.UserSettings;
import main.service.UserSettingsService;
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
        UserSettings.GameLanguage gameLanguage = UserSettingsService.getGameLanguage(userId);
        UserSettings.Category category = UserSettingsService.getCategory(userId);

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