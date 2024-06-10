package main.core.events;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LanguageButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final UserSettingsRepository userSettingsRepository;

    @Autowired
    public LanguageButton(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

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
        UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage.valueOf(language);
        UserSettings userSettings = userSettingsRepository.getByUserIdLong(userId);

        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUserIdLong(userId);

            userSettings.setCategory(UserSettings.Category.ALL);
            userSettings.setGameLanguage(UserSettings.GameLanguage.EN);
            userSettings.setBotLanguage(UserSettings.BotLanguage.EN);

            BotStartConfig.getMapGameCategory().put(userId, UserSettings.Category.ALL);
            BotStartConfig.getMapGameLanguages().put(userId, UserSettings.GameLanguage.EN);
            BotStartConfig.getMapLanguages().put(userId, UserSettings.BotLanguage.EN);
        }

        userSettings.setGameLanguage(gameLanguage);
        BotStartConfig.getMapGameLanguages().put(userId, gameLanguage);
        userSettingsRepository.save(userSettings);
    }
}
