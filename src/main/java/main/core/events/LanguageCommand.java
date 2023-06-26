package main.core.events;

import main.config.BotStartConfig;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.entity.Language;
import main.model.repository.GameLanguageRepository;
import main.model.repository.LanguageRepository;
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

    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;

    @Autowired
    public LanguageCommand(LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository) {
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
    }

    public void language(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

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
        //0 - game | 1 - bot
        if (event.getOptions().size() == 2) {
            String opOne = event.getOptions().get(0).getAsString();
            String opTwo = event.getOptions().get(1).getAsString();

            BotStartConfig.getMapGameLanguages().put(userIdLong, opOne);
            BotStartConfig.getMapLanguages().put(userIdLong, opTwo);

            String slashLanguage = String.format(jsonParsers.getLocale("slash_language", userIdLong), opOne, opTwo);

            event.reply(slashLanguage).addActionRow(HangmanUtils.BUTTON_PLAY_AGAIN).queue();

            GameLanguage gameLanguage = new GameLanguage();
            gameLanguage.setUserIdLong(userIdLong);
            gameLanguage.setLanguage(opOne);
            gameLanguageRepository.save(gameLanguage);

            Language language = new Language();
            language.setUserIdLong(userIdLong);
            language.setLanguage(opTwo);
            languageRepository.save(language);
        }
    }
}
