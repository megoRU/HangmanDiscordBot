package main.core.events;

import main.config.BotStartConfig;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CompetitivePlayButton {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    @Autowired
    public CompetitivePlayButton(CompetitiveQueueRepository competitiveQueueRepository) {
        this.competitiveQueueRepository = competitiveQueueRepository;
    }

    public void competitive(@NotNull ButtonInteractionEvent event) {
        var userId = event.getUser().getIdLong();
        boolean fromGuild = event.isFromGuild();
        long messageChannel = event.getMessageChannel().getIdLong();
        event.editButton(event.getButton().asDisabled()).queue();

        if (fromGuild) {
            String availableOnlyPm = jsonParsers.getLocale("available_only_pm", event.getUser().getIdLong());
            event.getHook().sendMessage(availableOnlyPm)
                    .setEphemeral(true)
                    .queue();
        } else {
            String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());

            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
            UserSettings userSettings = userSettingsMap.get(userId);

            if (userSettings == null) {
                event.getHook().sendMessage(gameLanguage)
                        .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                        .addActionRow(HangmanUtils.getButtonPlayCompetitiveAgain(userId))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            UserSettings.GameLanguage userGameLanguage = userSettings.getGameLanguage();

            if (!instance.hasCompetitive(userId) && !instance.hasHangman(userId)) {
                String addedToTheQueue = jsonParsers.getLocale("added_to_the_queue", event.getUser().getIdLong());

                CompetitiveQueue competitiveQueue = new CompetitiveQueue();
                competitiveQueue.setUserIdLong(userId);
                competitiveQueue.setGameLanguage(userGameLanguage);
                competitiveQueue.setMessageChannel(messageChannel);
                competitiveQueueRepository.save(competitiveQueue);

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userId, null, messageChannel, userGameLanguage);
                instance.addCompetitiveQueue(hangmanPlayer);

                event.getHook().sendMessage(addedToTheQueue)
                        .setActionRow(HangmanUtils.getButtonLeaveSearch(userId))
                        .queue();

            } else if (instance.hasHangman(userId)) {
                String youArePlayNow = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());
                event.getHook().sendMessage(youArePlayNow).queue();
            } else {
                String alreadyInQueue = jsonParsers.getLocale("already_in_queue", event.getUser().getIdLong());
                event.getHook().sendMessage(alreadyInQueue).queue();
            }
        }
    }
}