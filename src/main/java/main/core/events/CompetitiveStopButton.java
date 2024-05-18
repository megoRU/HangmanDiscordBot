package main.core.events;

import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.CompetitiveQueueRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompetitiveStopButton {

    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    @Autowired
    public CompetitiveStopButton(CompetitiveQueueRepository competitiveQueueRepository) {
        this.competitiveQueueRepository = competitiveQueueRepository;
    }

    public void stop(@NotNull ButtonInteractionEvent event) {
        long userIdLong = event.getUser().getIdLong();
        HangmanRegistry instance = HangmanRegistry.getInstance();
        instance.removeFromCompetitiveQueue(userIdLong);
        String deleteCompetitiveSearch = jsonParsers.getLocale("delete_competitive_search", userIdLong);
        event.reply(deleteCompetitiveSearch).queue();
        competitiveQueueRepository.deleteById(userIdLong);
        event.editButton(event.getButton().asDisabled()).queue();
    }
}