package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Statistic;
import main.model.repository.GamesRepository;
import main.statistic.CreatorGraph;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GetGlobalStatsInGraph extends ListenerAdapter {

    private static final String ALL_STATS = "!hg allstats";
    private static final String MY_STATS = "!hg mystats";
    private final GamesRepository gamesRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            if (!event.isFromType(ChannelType.TEXT)) return;

            if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) return;

            String message = event.getMessage().getContentRaw().trim().toLowerCase();

            String prefix = ALL_STATS;
            String prefix2 = MY_STATS;

            if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
                prefix = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "hg allstats";
                prefix2 = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "hg mystats";
            }

            if (message.equals(prefix)) {
                event.getMessage().getTextChannel().sendTyping().queue();

                CreatorGraph creatorGraph = new CreatorGraph(
                        gamesRepository,
                        event.getMessage().getTextChannel().getId(),
                        event.getAuthor().getId(),
                        event.getAuthor().getName(),
                        event.getAuthor().getAvatarUrl(),
                        null);
                creatorGraph.createGraph(Statistic.GLOBAL);
            }

            if (message.equals(prefix2)) {
                event.getMessage().getTextChannel().sendTyping().queue();

                CreatorGraph creatorGraph = new CreatorGraph(
                        gamesRepository,
                        event.getMessage().getTextChannel().getId(),
                        event.getAuthor().getId(),
                        event.getAuthor().getName(),
                        event.getAuthor().getAvatarUrl(),
                        null);
                creatorGraph.createGraph(Statistic.MY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}