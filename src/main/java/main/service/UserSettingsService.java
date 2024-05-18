package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public void settings() {
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();


        for (UserSettings userSettings : userSettingsList) {
            Long userIdLong = userSettings.getUserIdLong();
            UserSettings.BotLanguage botLanguage = userSettings.getBotLanguage();
            UserSettings.GameLanguage gameLanguage = userSettings.getGameLanguage();
            UserSettings.Category category = userSettings.getCategory();

            BotStartConfig.mapLanguages.put(userIdLong, botLanguage);
            BotStartConfig.mapGameLanguages.put(userIdLong, gameLanguage);
            BotStartConfig.mapGameCategory.put(userIdLong, category);
        }
        System.out.println("getUserSettings()");
    }
}