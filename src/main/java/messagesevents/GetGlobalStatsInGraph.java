package messagesevents;

import db.DataBase;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import statistic.CreatorGraph;
import statistic.Statistic;

public class GetGlobalStatsInGraph extends ListenerAdapter {

    private static final String ALL_STATS = "!hg allstats";
    private static final String MY_STATS = "!hg mystats";

    @SneakyThrows
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = ALL_STATS;
        String prefix2 = MY_STATS;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg allstats";
            prefix2 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg mystats";
        }

        if (message.equals(prefix)) {
            event.getMessage().getTextChannel().sendTyping().queue();
            CreatorGraph creatorGraph = new CreatorGraph(
                    DataBase.getInstance().getAllStatistic(),
                    event.getGuild().getId(),
                    event.getMessage().getTextChannel().getId(),
                    event.getAuthor().getId(),
                    event.getAuthor().getName(),
                    event.getAuthor().getAvatarUrl());
            creatorGraph.createGraph(Statistic.GLOBAL);
        }

        if (message.equals(prefix2)) {
            event.getMessage().getTextChannel().sendTyping().queue();
            CreatorGraph creatorGraph = new CreatorGraph(
                    DataBase.getInstance().getMyAllStatistic(event.getAuthor().getId()),
                    event.getGuild().getId(),
                    event.getMessage().getTextChannel().getId(),
                    event.getAuthor().getId(),
                    event.getAuthor().getName(),
                    event.getAuthor().getAvatarUrl());
            creatorGraph.createGraph(Statistic.MY);
        }

    }


}
