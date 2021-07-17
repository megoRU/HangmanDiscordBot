package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import java.text.DecimalFormat;

public class MessageStats extends ListenerAdapter {

    private static final String STATS = "!hg stats";
    private final JSONParsers jsonParsers = new JSONParsers();
    private static final DecimalFormat df = new DecimalFormat("##.#");

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = STATS;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg stats";
        }

        if (message.equals(prefix)) {
            sendStats(event.getChannel(), event.getAuthor().getAvatarUrl(), event.getAuthor().getId(), event.getAuthor().getName());
        }
    }

    public void sendStats(TextChannel textChannel, String userAvatarUrl, String userIdLong, String name) {
        String[] statistic = DataBase.getInstance().getStatistic(userIdLong).split(" ");

        if (statistic.length == 3) {
            //Формула:
            //Количество побед / Общее количество * Максимальное количество процентов
            if (Integer.parseInt(statistic[0]) == 0) {
                textChannel.sendMessage(jsonParsers.getLocale("MessageStats_Zero_Divide", userIdLong)).queue();
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

            EmbedBuilder stats = new EmbedBuilder();
            stats.setColor(0x00FF00);
            stats.setAuthor(name, null, avatarUrl);
            stats.setTitle(jsonParsers.getLocale("MessageStats_Your_Stats",
                    userIdLong));
            stats.setDescription(
                    jsonParsers.getLocale("MessageStats_Game_Count",
                            userIdLong).replaceAll("\\{0}", statistic[0]) +

                            jsonParsers.getLocale("MessageStats_Game_Wins",
                                    userIdLong).replaceAll("\\{0}", statistic[2]) +

                            jsonParsers.getLocale("MessageStats_Game_Percentage",
                                    userIdLong).replaceAll("\\{0}", df.format(percentage)));

            textChannel.sendMessageEmbeds(stats.build()).queue();
            stats.clear();
        }
    }
}