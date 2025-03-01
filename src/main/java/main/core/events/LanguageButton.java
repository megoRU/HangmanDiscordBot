package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
public class LanguageButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final UserSettingsRepository userSettingsRepository;

    public void language(@NotNull ButtonInteractionEvent event) {
        event.editButton(event.getButton().asDisabled()).queue();
        var userId = event.getUser().getIdLong();

        boolean hasHangman = HangmanRegistry.getInstance().hasHangman(userId);
        if (hasHangman) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());
            event.getHook().sendMessage(reactionsButtonWhenPlay)
                    .setEphemeral(true)
                    .setActionRow(HangmanUtils.getButtonStop(userId))
                    .queue();
            return;
        }

        String languageChangeLang;
        if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())) {
            setGameLanguage(userId, "RU");
            String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
            languageChangeLang = String.format(languageChange, "Кириллица");
        } else {
            setGameLanguage(userId, "EN");
            String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
            languageChangeLang = String.format(languageChange, "Latin");
        }
        event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
    }

    private void setGameLanguage(long userId, String language) {

        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userId);

        UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage.valueOf(language);

        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUserIdLong(userId);
            userSettings.setCategory(UserSettings.Category.ALL);
            userSettings.setBotLanguage(UserSettings.BotLanguage.EN);
        }

        userSettings.setGameLanguage(gameLanguage);
        userSettingsMap.put(userId, userSettings);
        userSettingsRepository.save(userSettings);
    }
}
