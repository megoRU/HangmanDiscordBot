package main.eventlisteners.buildClass;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;

@AllArgsConstructor
public class GameLanguageChange {

    private final GameLanguageRepository gameLanguageRepository;

    public void set(String message, Long userIdLong) {
        try {
            BotStartConfig.getMapGameLanguages().put(userIdLong, message);
            GameLanguage gameLanguage = new GameLanguage();
            gameLanguage.setUserIdLong(userIdLong);
            gameLanguage.setLanguage(message);
            gameLanguageRepository.save(gameLanguage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
