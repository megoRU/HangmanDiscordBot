package main.core.events;

import main.enums.Statistic;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.statistic.CreatorGraph;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@Service
public class StatsCommand {

    private final GamesRepository gamesRepository;

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final DecimalFormat df = new DecimalFormat("##.#");

    @Autowired
    public StatsCommand(GamesRepository gamesRepository) {
        this.gamesRepository = gamesRepository;
    }

    public void stats(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        CreatorGraph creatorGraph = new CreatorGraph(
                gamesRepository,
                event.getUser().getId(),
                event.getHook());

        switch (event.getName()) {
            case "allstats" -> creatorGraph.createGraph(Statistic.GLOBAL);
            case "mystats" -> creatorGraph.createGraph(Statistic.MY);
        }
    }

    public void stats(@NotNull InteractionHook interactionHook) {
        var userIdLong = interactionHook.getInteraction().getUser().getIdLong();
        var userName = interactionHook.getInteraction().getUser().getName();
        var userAvatarUrl = interactionHook.getInteraction().getUser().getAvatarUrl();

        try {
            String[] statistic = gamesRepository.getStatistic(userIdLong).replaceAll(",", " ").split(" ");

            if (statistic.length == 3) {
                //Формула:
                //Количество побед / Общее количество * Максимальное количество процентов
                if (Integer.parseInt(statistic[0]) == 0) {
                    String messageStatsZeroDivide = jsonParsers.getLocale("MessageStats_Zero_Divide", userIdLong);

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(userName, null, userAvatarUrl);
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(messageStatsZeroDivide);

                    interactionHook.sendMessageEmbeds(needSetLanguage.build()).queue();
                    return;
                }

                double percentage = (double) Integer.parseInt(statistic[2]) / Integer.parseInt(statistic[0]) * 100.0;
                String avatarUrl = null;

                if (userAvatarUrl == null) {
                    avatarUrl = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
                }
                if (userAvatarUrl != null) {
                    avatarUrl = userAvatarUrl;
                }

                String messageStatsYourStats = jsonParsers.getLocale("MessageStats_Your_Stats", userIdLong);
                String messageStatsGameCount = String.format(jsonParsers.getLocale("MessageStats_Game_Count", userIdLong), statistic[0]);
                String messageStatsGameWins = String.format(jsonParsers.getLocale("MessageStats_Game_Wins", userIdLong), statistic[2]);
                String messageStatsGamePercentage = String.format(jsonParsers.getLocale("MessageStats_Game_Percentage", userIdLong), df.format(percentage), "%");

                EmbedBuilder stats = new EmbedBuilder();
                stats.setColor(0x00FF00);
                stats.setAuthor(userName, null, avatarUrl);
                stats.setTitle(messageStatsYourStats);
                stats.setDescription(messageStatsGameCount + messageStatsGameWins + messageStatsGamePercentage);

                interactionHook.sendMessageEmbeds(stats.build()).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
