package main.core.events;

import main.config.BotStartConfig;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
public class LanguageCommand {

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final UserSettingsRepository userSettingsRepository;

    @Autowired
    public LanguageCommand(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void language(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

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

        //Если игрок сейчас играет сменить язык не даст
        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);
            event.reply(reactionsButtonWhenPlay)
                    .setActionRow(HangmanUtils.getButtonStop(userIdLong))
                    .setEphemeral(true)
                    .queue();
            return;
        } //0 - game | 1 - bot
        else if (event.getOptions().size() == 2) {
            UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage
                    .valueOf(event.getOptions().get(0).getAsString().toUpperCase());

            UserSettings.BotLanguage botLanguage = UserSettings.BotLanguage
                    .valueOf(event.getOptions().get(1).getAsString().toUpperCase());

            String opOne = event.getOptions().get(0).getAsString();
            String opTwo = event.getOptions().get(1).getAsString();

            BotStartConfig.getMapGameLanguages().put(userIdLong, gameLanguage);
            BotStartConfig.getMapLanguages().put(userIdLong, botLanguage);

            String slashLanguage = String.format(jsonParsers.getLocale("slash_language", userIdLong), opOne, opTwo);

            event.reply(slashLanguage)
                    .setActionRow(HangmanUtils.getButtonPlayAgain(userIdLong))
                    .setEphemeral(true)
                    .queue();

            userSettings.setBotLanguage(botLanguage);
            userSettings.setGameLanguage(gameLanguage);
            userSettingsRepository.save(userSettings);
        }
    }
}
