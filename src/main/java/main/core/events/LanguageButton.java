package main.core.events;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LanguageButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final GameLanguageRepository gameLanguageRepository;

    @Autowired
    public LanguageButton(GameLanguageRepository gameLanguageRepository) {
        this.gameLanguageRepository = gameLanguageRepository;
    }

    public void language(@NotNull ButtonInteractionEvent event) {
        event.editButton(event.getButton().asDisabled()).queue();
        var userId = event.getUser().getIdLong();

        boolean hasHangman = HangmanRegistry.getInstance().hasHangman(userId);
        if (hasHangman) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());
            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(0x00FF00);
            youPlay.setDescription(reactionsButtonWhenPlay);
            event.getHook().sendMessageEmbeds(youPlay.build()).setEphemeral(true).addActionRow(HangmanUtils.BUTTON_STOP).queue();
            return;
        }

        String languageChangeLang;
        if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())) {
            setGameLanguage(userId, "rus");
            String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
            languageChangeLang = String.format(languageChange, "Кириллица");
        } else {
            setGameLanguage(userId, "eng");
            String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
            languageChangeLang = String.format(languageChange, "Latin");
        }
        event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
    }

    private void setGameLanguage(long userId, String language) {
        BotStartConfig.getMapGameLanguages().put(userId, language);
        GameLanguage gameLanguage = new GameLanguage();
        gameLanguage.setUserIdLong(userId);
        gameLanguage.setLanguage(language);
        gameLanguageRepository.save(gameLanguage);
    }
}
