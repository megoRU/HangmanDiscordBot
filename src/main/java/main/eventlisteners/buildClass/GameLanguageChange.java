package main.eventlisteners.buildClass;

import main.config.BotStartConfig;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;

public class GameLanguageChange {

    private final GameLanguageRepository gameLanguageRepository;

    public GameLanguageChange(GameLanguageRepository gameLanguageRepository) {
        this.gameLanguageRepository = gameLanguageRepository;
    }

    public void set(String message, String userIdLong) {
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
