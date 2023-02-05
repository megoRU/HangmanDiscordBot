package main.core.events;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;
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
