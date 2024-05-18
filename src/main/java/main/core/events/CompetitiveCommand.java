package main.core.events;

import main.controller.UpdateController;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import main.service.UserSettingsService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompetitiveCommand {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final UserSettingsService userSettingsService;
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    @Autowired
    public CompetitiveCommand(CompetitiveQueueRepository competitiveQueueRepository,
                              UserSettingsService userSettingsService) {
        this.competitiveQueueRepository = competitiveQueueRepository;
        this.userSettingsService = userSettingsService;
    }

    public void competitive(@NotNull SlashCommandInteractionEvent event, UpdateController updateController) {
        var userId = event.getUser().getIdLong();
        boolean fromGuild = event.isFromGuild();
        long messageChannel = event.getMessageChannel().getIdLong();

        if (fromGuild) {
            String competitiveMessage = jsonParsers.getLocale("competitive_message", event.getUser().getIdLong());

            String availableOnlyPm = jsonParsers.getLocale("available_only_pm", event.getUser().getIdLong());
            event.reply(availableOnlyPm)
                    .setEphemeral(true)
                    .queue();

            updateController.sendMessage(String.valueOf(userId), competitiveMessage);
        } else {
            String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
            UserSettings.GameLanguage userGameLanguage = userSettingsService.getUserGameLanguage(userId);

            if (userGameLanguage == null) {
                event.reply(gameLanguage)
                        .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                        .addActionRow(HangmanUtils.getButtonPlayCompetitiveAgain(userId))
                        .setEphemeral(true)
                        .queue();
                return;
            }
            HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();

            if (!hangmanRegistry.hasCompetitive(userId) && !hangmanRegistry.hasHangman(userId)) {
                String addedToTheQueue = jsonParsers.getLocale("added_to_the_queue", event.getUser().getIdLong());

                CompetitiveQueue competitiveQueue = new CompetitiveQueue();
                competitiveQueue.setUserIdLong(userId);
                competitiveQueue.setGameLanguage(userGameLanguage);
                competitiveQueue.setMessageChannel(messageChannel);
                competitiveQueueRepository.save(competitiveQueue);

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userId, null, messageChannel, userGameLanguage);
                hangmanRegistry.addCompetitiveQueue(hangmanPlayer);

                event.reply(addedToTheQueue)
                        .setActionRow(HangmanUtils.getButtonLeaveSearch(userId))
                        .queue();

            } else if (hangmanRegistry.hasHangman(userId)) {
                String youArePlayNow = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());
                event.reply(youArePlayNow)
                        .setActionRow(HangmanUtils.getButtonStop(userId))
                        .queue();
            } else {
                String alreadyInQueue = jsonParsers.getLocale("already_in_queue", event.getUser().getIdLong());
                event.reply(alreadyInQueue).queue();
            }
        }
    }
}