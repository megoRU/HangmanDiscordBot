package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.sql.ResultSet;

public class GetGlobalStatsInGraph extends ListenerAdapter {

    private static final String STATS = "!hg allstats";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final StringBuilder dataName = new StringBuilder();
    private final StringBuilder count = new StringBuilder();

    @SneakyThrows
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }
        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = STATS;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg allstats";
        }

        if (message.equals(prefix)) {

            ResultSet resultSet = DataBase.getInstance().getAllStatistic();

            while (resultSet.next()) {
                dataName.append(dataName.length() == 0 ? "" : ",").append("%27").append(resultSet.getString("game_date"), 0, 10).append("%27");
                count.append(count.length() == 0 ? "" : ",").append("%27").append(resultSet.getString("count")).append("%27");
            }

            System.out.println(dataName);

            System.out.println(count);

            final String url = "https://quickchart.io/chart?bkg=white&c={%20type:%20%27line%27,%20data:%20{%20labels:%20[" + dataName
                    + "],%20datasets:%20[%20{%20label:%20%27Count%27,%20backgroundColor:%20%27rgb(255,%2099,%20132)%27,%20borderColor:%20%27rgb(255,%2099,%20132)%27,%20" +
                    "data:%20[" + count + "],%20fill:%20%27fill:%20start%27,%20}%20],%20},%20options%3A%20%7B%0A%20%20%0A%20%20%20%20plugins%3A%20%7B%0A%20%20%20%20%20%20datalabels%3A%20%7B%0A%20%20%20%20%20%20%20%20anchor%3A%20%27center%27%2C%0A%20%20%20%20%20%20%20%20align%3A%20%27top%27%2C%0A%20%20%20%20%20%20%20%20color%3A%20%27%23000000%27%2C%0A%20%20%20%20%20%20%20%20font%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20weight%3A%20%27bold%27%2C%0A%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%7D%2C%0A%20%20%0A%20%20%20%20title%3A%20%7B%0A%20%20%20%20%20%20display%3A%20true%2C%0A%20%20%20%20%20%20text%3A%20%27Hangman%20games%20by%20month%27%2C%0A%20%20%20%20%7D%2C%0A%20%20%7D%2C%0A%7D%0A";

            EmbedBuilder globalStats = new EmbedBuilder();
            globalStats.setColor(0x00FF00);
            globalStats.setTitle(jsonParsers.getLocale("MessageStats_All_Stats", event.getAuthor().getId()));
            globalStats.setImage(url);
            event.getChannel().sendMessageEmbeds(globalStats.build()).queue();

        }
    }


}
