package main.core.events;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
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

        if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());
            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(0x00FF00);
            youPlay.setDescription(reactionsButtonWhenPlay);
            event.getHook().sendMessageEmbeds(youPlay.build()).setEphemeral(true).addActionRow(HangmanUtils.BUTTON_STOP).queue();
        } else {
            if (event.getButton().getEmoji() != null) {
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "RU" : "EN";
                String reactionsButton = jsonParsers.getLocale("ReactionsButton_Save", event.getUser().getIdLong());
                String reactionsButtonSave = String.format(reactionsButton, event.getButton().getLabel());

                UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage.valueOf(buttonName);
                BotStartConfig.getMapGameLanguages().put(event.getUser().getIdLong(), gameLanguage);
                event.getHook().sendMessage(reactionsButtonSave).setEphemeral(true).queue();

                UserSettings userSettings = userSettingsRepository.getByUserIdLong(event.getUser().getIdLong());
                userSettings.setGameLanguage(gameLanguage);
                userSettingsRepository.save(userSettings);
            }
        }
    }
}