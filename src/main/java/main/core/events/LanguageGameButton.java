package main.core.events;

import main.config.BotStartConfig;
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

@Service
public class LanguageGameButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final UserSettingsRepository userSettingsRepository;

    @Autowired
    public LanguageGameButton(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void language(@NotNull ButtonInteractionEvent event) {
        event.editButton(event.getButton().asDisabled()).queue();
        long userIdLong = event.getUser().getIdLong();

        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);
            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(0x00FF00);
            youPlay.setDescription(reactionsButtonWhenPlay);
            event.getHook().sendMessageEmbeds(youPlay.build()).setEphemeral(true).addActionRow(HangmanUtils.BUTTON_STOP).queue();
        } else {
            if (event.getButton().getEmoji() != null) {
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "RU" : "EN";
                String reactionsButton = jsonParsers.getLocale("ReactionsButton_Save", userIdLong);
                String reactionsButtonSave = String.format(reactionsButton, event.getButton().getLabel());

                UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage.valueOf(buttonName);
                event.getHook().sendMessage(reactionsButtonSave).setEphemeral(true).queue();

                UserSettings userSettings = userSettingsRepository.getByUserIdLong(userIdLong);

                if (userSettings == null) {
                    userSettings = new UserSettings();
                    userSettings.setUserIdLong(userIdLong);

                    userSettings.setCategory(UserSettings.Category.ALL);
                    userSettings.setGameLanguage(UserSettings.GameLanguage.EN);
                    userSettings.setBotLanguage(UserSettings.BotLanguage.EN);

                    BotStartConfig.getMapGameCategory().put(userIdLong, UserSettings.Category.ALL);
                    BotStartConfig.getMapGameLanguages().put(userIdLong, UserSettings.GameLanguage.EN);
                    BotStartConfig.getMapLanguages().put(userIdLong, UserSettings.BotLanguage.EN);
                }

                userSettings.setGameLanguage(gameLanguage);
                BotStartConfig.getMapGameLanguages().put(userIdLong, gameLanguage);
                userSettingsRepository.save(userSettings);
            }
        }
    }
}