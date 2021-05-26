package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MessageGlobalStats extends ListenerAdapter {

    private static final String STATS = "!hg global stats";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final DecimalFormat df = new DecimalFormat("##.#");
    private final ArrayList<String> setUsers = new ArrayList<>();
    private final ArrayList<GameStatsByUser> gameStatsByUsers = new ArrayList<>();
    private StringBuilder insertQuery = new StringBuilder();

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
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg global stats";
        }

        String userIdLong = event.getAuthor().getId();

        if (message.equals(prefix)) {
            getSetUsers();

            for (int i = 0; i < setUsers.size(); i++) {
                insertQuery.append(insertQuery.length() == 0 ? "" : " UNION ")
                        .append("SELECT COUNT(games_id) AS COUNT_GAMES, "
                                + "user_id_long AS USER_ID_LONG, "
                                + "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS, "
                                + "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES "
                                + "FROM player, games WHERE player.user_id_long = ")
                        .append(setUsers.get(i)).append(" AND player.games_id = games.id");
            }

            getStatistic();

            int n = gameStatsByUsers.size();
            GameStatsByUser temp;
            for (int i = 0; i < n; i++) {
                for (int j = 1; j < n - i; j++) {
                    if (gameStatsByUsers.get(j - 1).getPercentage() > gameStatsByUsers.get(j).getPercentage()) {
                        temp = gameStatsByUsers.get(j - 1);
                        gameStatsByUsers.set(j - 1, gameStatsByUsers.get(j));
                        gameStatsByUsers.set(j, temp);
                    }
                }
            }

            EmbedBuilder globalStats = new EmbedBuilder();
            globalStats.setColor(0x00FF00);
            globalStats.setTitle(jsonParsers.getLocale("MessageStats_Your_Stats",
                    event.getAuthor().getId()));
            globalStats.setDescription(
                    "First place: <@" + gameStatsByUsers.get(gameStatsByUsers.size() - 1).getID_LONG() + "> `" + gameStatsByUsers.get(gameStatsByUsers.size() - 1).getPercentage() + "%`\n " +
                    "Second place: <@" + gameStatsByUsers.get(gameStatsByUsers.size() - 2).getID_LONG() + "> `" + gameStatsByUsers.get(gameStatsByUsers.size() - 2).getPercentage() + "%`\n " +
                    "Third  place: <@" + gameStatsByUsers.get(gameStatsByUsers.size() - 3).getID_LONG() + "> `" + gameStatsByUsers.get(gameStatsByUsers.size() - 3).getPercentage() + "%`");
            event.getChannel().sendMessage(globalStats.build()).queue();
            globalStats.clear();

            insertQuery = new StringBuilder();
            gameStatsByUsers.clear();
            setUsers.clear();
        }
    }

    public void getSetUsers() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT DISTINCT user_id_long FROM `player`";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                setUsers.add(rs.getString("user_id_long"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getStatistic() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            ResultSet rs = statement.executeQuery(insertQuery.toString());
            while (rs.next()) {
                double percentage =
                        (double) Integer.parseInt(rs.getString("TOTAL_ONES")) /
                                Integer.parseInt(rs.getString("COUNT_GAMES")) * 100.0;
                gameStatsByUsers.add(new GameStatsByUser(
                        Double.parseDouble(df.format(percentage).replaceAll(",", ".")),
                        rs.getString("USER_ID_LONG"),
                        rs.getString("COUNT_GAMES")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

