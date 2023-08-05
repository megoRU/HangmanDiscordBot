package main.core.events;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
public class CategoryCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final UserSettingsRepository userSettingsRepository;

    @Autowired
    public CategoryCommand(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void category(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

        String categorySlash = event.getOption("category", OptionMapping::getAsString);
        String gameCategory = jsonParsers.getLocale("game_category", userIdLong);

        //Если игрок сейчас играет сменить язык не даст
        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);

            EmbedBuilder whenPlay = new EmbedBuilder();

            whenPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            whenPlay.setColor(Color.GREEN);
            whenPlay.setDescription(reactionsButtonWhenPlay);

            event.replyEmbeds(whenPlay.build()).addActionRow(HangmanUtils.BUTTON_STOP).queue();
            return;
        }

        UserSettings userSettings = userSettingsRepository.getByUserIdLong(userIdLong);

        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUserIdLong(userIdLong);
            userSettings.setCategory(UserSettings.Category.ALL);
            userSettings.setGameLanguage(UserSettings.GameLanguage.EN);
            userSettings.setBotLanguage(UserSettings.BotLanguage.EN);
        }

        if (categorySlash != null && categorySlash.equals("any")) {
            BotStartConfig.mapGameCategory.put(userIdLong, UserSettings.Category.ALL);
            event.reply(gameCategory).setEphemeral(true).queue();

            userSettings.setCategory(UserSettings.Category.ALL);
            userSettingsRepository.save(userSettings);
        } else if (categorySlash != null) {
            BotStartConfig.mapGameCategory.put(userIdLong, UserSettings.Category.valueOf(categorySlash.toUpperCase()));
            event.reply(gameCategory).setEphemeral(true).queue();

            userSettings.setCategory(UserSettings.Category.valueOf(categorySlash.toUpperCase()));
            userSettingsRepository.save(userSettings);
        }
    }
}
