package main.service;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private static final Map<Long, UserSettings> userSettingsMap = new ConcurrentHashMap<>();


    public static UserSettings.Category getCategory(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) return null;
        return userSettings.getCategory();
    }

    public static UserSettings.GameLanguage getGameLanguage(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) return null;
        return userSettings.getGameLanguage();
    }

    public static UserSettings.BotLanguage getLanguage(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) return null;
        return userSettings.getBotLanguage();
    }

    @Nullable
    public UserSettings.GameLanguage getUserGameLanguage(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) {
            userSettings = userSettingsRepository.getByUserIdLong(userId);
            if (userSettings == null) {
                return null;
            } else {
                userSettingsMap.put(userId, userSettings);
                return userSettings.getGameLanguage();
            }
        }
        return userSettings.getGameLanguage();
    }

    @Nullable
    public UserSettings.BotLanguage getUserLanguage(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) {
            userSettings = userSettingsRepository.getByUserIdLong(userId);
            if (userSettings == null) {
                return null;
            } else {
                userSettingsMap.put(userId, userSettings);
                return userSettings.getBotLanguage();
            }
        }
        return userSettings.getBotLanguage();
    }

    @Nullable
    public UserSettings.Category getUserCategory(Long userId) {
        UserSettings userSettings = userSettingsMap.get(userId);
        if (userSettings == null) {
            userSettings = userSettingsRepository.getByUserIdLong(userId);
            if (userSettings == null) {
                return null;
            } else {
                userSettingsMap.put(userId, userSettings);
                return userSettings.getCategory();
            }
        }
        return userSettings.getCategory();
    }
}