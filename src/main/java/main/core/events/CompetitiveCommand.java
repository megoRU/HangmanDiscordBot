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
            String availableOnlyPm = jsonParsers.getLocale("available_only_pm", event.getUser().getIdLong());
            event.reply(availableOnlyPm)
                    .setEphemeral(true)
                    .queue();
        } else {
            String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
            UserSettings.GameLanguage userGameLanguage = BotStartConfig.getMapGameLanguages().get(userIdLong);

            if (userGameLanguage == null) {
                event.reply(gameLanguage)
                        .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                        .addActionRow(HangmanUtils.BUTTON_PLAY_AGAIN)
                        .setEphemeral(true)
                        .queue();
                return;
            }
            HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();

            if (!hangmanRegistry.hasCompetitive(userIdLong) && !hangmanRegistry.hasHangman(userIdLong)) {
                String addedToTheQueue = jsonParsers.getLocale("added_to_the_queue", event.getUser().getIdLong());

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, userGameLanguage);
                hangmanRegistry.addCompetitiveQueue(hangmanPlayer);
                event.reply(addedToTheQueue)
                        .setEphemeral(true)
                        .queue();
            } else if (hangmanRegistry.hasHangman(userIdLong)) {
                String youArePlayNow = jsonParsers.getLocale("you_are_play_now", event.getUser().getIdLong());
                event.reply(youArePlayNow)
                        .setEphemeral(true)
                        .queue();
            } else {
                String alreadyInQueue = jsonParsers.getLocale("already_in_queue", event.getUser().getIdLong());

                event.reply(alreadyInQueue)
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}