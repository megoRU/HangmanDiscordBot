package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public void settings() {
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;

        for (UserSettings userSettings : userSettingsList) {
            Long userIdLong = userSettings.getUserIdLong();
            userSettingsMap.put(userIdLong, userSettings);
        }
        System.out.println("getUserSettings()");
    }
}