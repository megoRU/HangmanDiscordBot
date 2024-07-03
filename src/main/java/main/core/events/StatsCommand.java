package main.core.events;

import main.enums.Statistic;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.statistic.CreatorGraph;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class StatsCommand {

    private final GamesRepository gamesRepository;

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final DecimalFormat df = new DecimalFormat("##.#");
    private final static Logger LOGGER = Logger.getLogger(StatsCommand.class.getName());

    @Autowired
    public StatsCommand(GamesRepository gamesRepository) {
        this.gamesRepository = gamesRepository;
    }

    public void stats(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
            slashCommandInteractionEvent.deferReply().setEphemeral(true).queue();

            CreatorGraph creatorGraph = new CreatorGraph(gamesRepository,
                    slashCommandInteractionEvent.getUser().getId(),
                    slashCommandInteractionEvent.getHook());

            switch (slashCommandInteractionEvent.getName()) {
                case "bot-statistics" -> creatorGraph.createGraph(Statistic.GLOBAL);
                case "statistics" -> creatorGraph.createGraph(Statistic.MY);
            }
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            buttonInteractionEvent.editButton(buttonInteractionEvent.getButton().asDisabled()).queue();

            CreatorGraph creatorGraph = new CreatorGraph(gamesRepository,
                    buttonInteractionEvent.getUser().getId(),
                    buttonInteractionEvent.getHook());
            creatorGraph.createGraph(Statistic.MY);
        }
    }

    public void stats(EmbedBuilder embedBuilder, Long userId) {
        try {
            String[] statistic = gamesRepository.getStatistic(userId).replaceAll(",", " ").split(" ");

            if (statistic.length == 3) {
                //Формула:
                //Количество побед / Общее количество * Максимальное количество процентов
                if (Integer.parseInt(statistic[0]) == 0) {
                    String messageStatsZeroDivide = jsonParsers.getLocale("MessageStats_Zero_Divide", userId);
                    embedBuilder.setColor(0x00FF00);
                    embedBuilder.setDescription(messageStatsZeroDivide);
                    return;
                }

                double percentage = (double) Integer.parseInt(statistic[2]) / Integer.parseInt(statistic[0]) * 100.0;

                String messageStatsGameCount = String.format(jsonParsers.getLocale("MessageStats_Game_Count", userId), statistic[0]);
                String messageStatsGameWins = String.format(jsonParsers.getLocale("MessageStats_Game_Wins", userId), statistic[2]);
                String messageStatsGamePercentage = String.format(jsonParsers.getLocale("MessageStats_Game_Percentage", userId), df.format(percentage), "%");

                embedBuilder.setColor(0x00FF00);
                embedBuilder.setDescription(messageStatsGameCount + messageStatsGameWins + messageStatsGamePercentage);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }
}
