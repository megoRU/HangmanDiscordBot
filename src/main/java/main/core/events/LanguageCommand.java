package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class LanguageCommand {

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final UserSettingsRepository userSettingsRepository;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    public void language(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userIdLong);

        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUserIdLong(userIdLong);
            userSettings.setCategory(UserSettings.Category.ALL);
            userSettings.setGameLanguage(UserSettings.GameLanguage.EN);
            userSettings.setBotLanguage(UserSettings.BotLanguage.EN);
        }

        //Если игрок сейчас играет сменить язык не даст
        if (instance.hasHangman(userIdLong)) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);
            event.reply(reactionsButtonWhenPlay)
                    .setActionRow(HangmanUtils.getButtonStop(userIdLong))
                    .setEphemeral(true)
                    .queue();
        } //0 - game | 1 - bot
        else if (event.getOptions().size() == 2) {
            UserSettings.GameLanguage gameLanguage = UserSettings.GameLanguage
                    .valueOf(event.getOptions().get(0).getAsString().toUpperCase());

            UserSettings.BotLanguage botLanguage = UserSettings.BotLanguage
                    .valueOf(event.getOptions().get(1).getAsString().toUpperCase());

            userSettings.setBotLanguage(botLanguage);
            userSettings.setGameLanguage(gameLanguage);
            userSettingsMap.put(userIdLong, userSettings);

            String opOne = event.getOptions().get(0).getAsString();
            String opTwo = event.getOptions().get(1).getAsString();
            String slashLanguage = String.format(jsonParsers.getLocale("slash_language", userIdLong), opOne, opTwo);

            event.reply(slashLanguage).setEphemeral(true).queue();

            userSettingsRepository.save(userSettings);
        }
    }
}
