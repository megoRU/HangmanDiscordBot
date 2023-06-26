package main.core.events;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LanguageGameButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final GameLanguageRepository gameLanguageRepository;

    @Autowired
    public LanguageGameButton(GameLanguageRepository gameLanguageRepository) {
        this.gameLanguageRepository = gameLanguageRepository;
    }

    public void language(@NotNull ButtonInteractionEvent event) {
        event.editButton(event.getButton().asDisabled()).queue();

        if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());
            event.getHook().sendMessage(reactionsButtonWhenPlay).addActionRow(HangmanUtils.BUTTON_STOP).setEphemeral(true).queue();
        } else {
            if (event.getButton().getEmoji() != null) {
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                String reactionsButton = jsonParsers.getLocale("ReactionsButton_Save", event.getUser().getIdLong());
                String reactionsButtonSave = String.format(reactionsButton, event.getButton().getLabel());
                BotStartConfig.getMapGameLanguages().put(event.getUser().getIdLong(), buttonName);
                event.getHook().sendMessage(reactionsButtonSave).setEphemeral(true).queue();

                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(event.getUser().getIdLong());
                gameLanguage.setLanguage(buttonName);
                gameLanguageRepository.save(gameLanguage);
            }
        }
    }
}