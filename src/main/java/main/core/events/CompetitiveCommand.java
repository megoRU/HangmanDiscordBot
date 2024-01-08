package main.core.events;

import main.config.BotStartConfig;
import main.hangman.HangmanPlayer;
import main.hangman.HangmanRegistry;
import main.hangman.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class CompetitiveCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void competitive(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        boolean fromGuild = event.isFromGuild();
        long messageChannel = event.getMessageChannel().getIdLong();

        if (fromGuild) {
            event.reply("Run this command in private messages with me!")
                    .setEphemeral(true)
                    .queue();
        } else {
            String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
            UserSettings.GameLanguage userGameLanguage = BotStartConfig.getMapGameLanguages().get(userIdLong);

            if (userGameLanguage == null) {
                event.getHook().sendMessage(gameLanguage)
                        .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                        .addActionRow(HangmanUtils.BUTTON_PLAY_AGAIN)
                        .setEphemeral(true)
                        .queue();
                return;
            }
            HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();

            if (!hangmanRegistry.hasCompetitive(userIdLong)) {
                HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, userGameLanguage);
                hangmanRegistry.addCompetitiveQueue(hangmanPlayer);
                event.reply("Added you to the queue. I'll notify you when the game starts!")
                        .setEphemeral(true)
                        .queue();
            } else {
                event.reply("You are already in the queue!")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}