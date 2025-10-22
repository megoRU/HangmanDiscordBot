package main.core.events;

import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CompetitiveCommand {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    @Autowired
    public CompetitiveCommand(CompetitiveQueueRepository competitiveQueueRepository) {
        this.competitiveQueueRepository = competitiveQueueRepository;
    }

    public void competitive(@NotNull SlashCommandInteractionEvent event, UpdateController updateController) {
        User user = event.getUser();
        var userId = user.getIdLong();
        boolean fromGuild = event.isFromGuild();
        long messageChannel = event.getMessageChannel().getIdLong();

        if (fromGuild) {
            Long competitive = BotStartConfig.getCommandId("competitive");

            String competitiveMessage = String.format(jsonParsers.getLocale("competitive_message", user.getIdLong()), competitive);

            String availableOnlyPm = jsonParsers.getLocale("available_only_pm", user.getIdLong());
            event.reply(availableOnlyPm)
                    .setEphemeral(true)
                    .queue();

            updateController.sendMessage(user, competitiveMessage);
        } else {
            String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", user.getIdLong());

            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
            UserSettings userSettings = userSettingsMap.get(userId);

            if (userSettings == null) {
                event.reply(gameLanguage)
                        .addComponents(ActionRow.of(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH))
                        .addComponents(ActionRow.of(HangmanUtils.getButtonPlayCompetitiveAgain(userId)))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            UserSettings.GameLanguage userGameLanguage = userSettings.getGameLanguage();


            if (!instance.hasCompetitive(userId) && !instance.hasHangman(userId)) {
                String addedToTheQueue = jsonParsers.getLocale("added_to_the_queue", user.getIdLong());

                CompetitiveQueue competitiveQueue = new CompetitiveQueue();
                competitiveQueue.setUserIdLong(userId);
                competitiveQueue.setGameLanguage(userGameLanguage);
                competitiveQueue.setMessageChannel(messageChannel);
                competitiveQueueRepository.save(competitiveQueue);

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userId, null, messageChannel, userGameLanguage);
                instance.addCompetitiveQueue(hangmanPlayer);

                event.reply(addedToTheQueue)
                        .setComponents(ActionRow.of(HangmanUtils.getButtonLeaveSearch(userId)))
                        .queue();

            } else if (instance.hasHangman(userId)) {
                String youArePlayNow = jsonParsers.getLocale("Hangman_Listener_You_Play", user.getIdLong());
                event.reply(youArePlayNow)
                        .setComponents(ActionRow.of(HangmanUtils.getButtonStop(userId)))
                        .queue();
            } else {
                String alreadyInQueue = jsonParsers.getLocale("already_in_queue", user.getIdLong());
                event.reply(alreadyInQueue).queue();
            }
        }
    }
}