package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class CategoryCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final UserSettingsRepository userSettingsRepository;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    public void category(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

        event.deferReply().setEphemeral(true).queue();

        String categorySlash = event.getOption("select", OptionMapping::getAsString);
        String gameCategory = jsonParsers.getLocale("game_category", userIdLong);

        //Если игрок сейчас играет сменить язык не даст
        if (instance.hasHangman(userIdLong)) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);
            event.getHook().sendMessage(reactionsButtonWhenPlay)
                    .setActionRow(HangmanUtils.getButtonStop(userIdLong))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;

        UserSettings userSettings = userSettingsMap.get(userIdLong);

        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUserIdLong(userIdLong);
            userSettings.setCategory(UserSettings.Category.ALL);
            userSettings.setGameLanguage(UserSettings.GameLanguage.EN);
            userSettings.setBotLanguage(UserSettings.BotLanguage.EN);
        }

        if (categorySlash != null && categorySlash.equals("any")) {
            userSettings.setCategory(UserSettings.Category.ALL);
            userSettingsMap.put(userIdLong, userSettings);

            event.getHook().sendMessage(gameCategory).setEphemeral(true).queue();
            userSettingsRepository.save(userSettings);
        } else if (categorySlash != null) {
            userSettings.setCategory(UserSettings.Category.valueOf(categorySlash.toUpperCase()));
            userSettingsMap.put(userIdLong, userSettings);

            event.getHook().sendMessage(gameCategory).setEphemeral(true).queue();
            userSettingsRepository.save(userSettings);
        }
    }
}
