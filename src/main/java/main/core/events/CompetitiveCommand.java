package main.core.events;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompetitiveCommand {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    @Autowired
    public CompetitiveCommand(CompetitiveQueueRepository competitiveQueueRepository) {
        this.competitiveQueueRepository = competitiveQueueRepository;
    }

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

                CompetitiveQueue competitiveQueue = new CompetitiveQueue();
                competitiveQueue.setUserIdLong(userIdLong);
                competitiveQueue.setGameLanguage(userGameLanguage);
                competitiveQueue.setMessageChannel(messageChannel);
                competitiveQueueRepository.save(competitiveQueue);

                event.reply(addedToTheQueue)
                        .setActionRow(Button.danger(Buttons.BUTTON_COMPETITIVE_STOP.name(), "Cancel Matchmaking"))
                        .queue();
            } else if (hangmanRegistry.hasHangman(userIdLong)) {
                String youArePlayNow = jsonParsers.getLocale("you_are_play_now", event.getUser().getIdLong());
                event.reply(youArePlayNow).queue();
            } else {
                String alreadyInQueue = jsonParsers.getLocale("already_in_queue", event.getUser().getIdLong());
                event.reply(alreadyInQueue).queue();
            }
        }
    }
}