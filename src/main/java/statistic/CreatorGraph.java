package statistic;

import io.quickchart.QuickChart;
import jsonparser.JSONParsers;
import lombok.Getter;
import messagesevents.SenderMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import startbot.BotStart;

import java.sql.ResultSet;

@Getter
public class CreatorGraph implements SenderMessage {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ResultSet resultSet;
    private final String guildIdLong;
    private final String textChannelIdLong;
    private final String userIdLong;
    private final String userName;
    private final String userAvatarUrl;
    private final StringBuilder date = new StringBuilder();
    private final StringBuilder columnFirst = new StringBuilder();
    private final StringBuilder columnSecond = new StringBuilder();

    public CreatorGraph(ResultSet resultSet, String guildIdLong, String textChannelIdLong, String userIdLong, String userName, String userUrl) {
        this.resultSet = resultSet;
        this.guildIdLong = guildIdLong;
        this.textChannelIdLong = textChannelIdLong;
        this.userIdLong = userIdLong;
        this.userName = userName;
        this.userAvatarUrl = userUrl;
    }

    public void createGraph(Statistic statistic) {
        try {
            QuickChart chart = new QuickChart();
            chart.setBackgroundColor("white");
            chart.setWidth(800);
            chart.setHeight(400);

            switch (statistic) {
                case GLOBAL -> {
                    while (resultSet.next()) {
                        date.append(date.length() == 0 ? "" : ",").append("'").append(resultSet.getString("game_date"), 0, 10).append("'");
                        columnFirst.append(columnFirst.length() == 0 ? "" : ",").append("'").append(resultSet.getString("count")).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }
                case MY -> {
                    while (resultSet.next()) {
                        date.append(date.length() == 0 ? "" : ",").append("'").append(resultSet.getString("game_date"), 0, 10).append("'");
                        columnFirst.append(columnFirst.length() == 0 ? "" : ",").append("'").append(resultSet.getString("TOTAL_ONES")).append("'");
                        columnSecond.append(columnSecond.length() == 0 ? "" : ",").append("'").append(resultSet.getString("TOTAL_ZEROS")).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }

            }
            EmbedBuilder globalStats = new EmbedBuilder();

            if (statistic.name().equals("MY")) {
                String avatar = null;
                if (userAvatarUrl == null) {
                    avatar = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
                }
                if (userAvatarUrl != null) {
                    avatar = userAvatarUrl;
                }
                globalStats.setAuthor(userName, null, avatar);
            }

            globalStats.setColor(0x00FF00);

            globalStats.setTitle(jsonParsers.getLocale("MessageStats_All_Stats", userIdLong));
            globalStats.setImage(chart.getShortUrl());

            sendMessage(globalStats, BotStart.getJda().getTextChannelById(textChannelIdLong));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private QuickChart setImage(QuickChart chart, Statistic statistic) {
        switch (statistic) {
            case GLOBAL -> chart.setConfig("{" +
                    "  type: 'line'," +
                    "  data: {" +
                    "    labels: [" + date + "]," +
                    "    datasets: [" +
                    "      {" +
                    "        label: 'Count games'," +
                    "        backgroundColor: 'rgb(255, 99, 132)'," +
                    "        borderColor: 'rgb(255, 99, 132)'," +
                    "        data: [" + columnFirst + "]," +
                    "        fill: false," +
                    "      }," +
                    "    " +
                    "    ]," +
                    "  }," +
                    "  options: {" +
                    "    plugins: {" +
                    "      datalabels: {" +
                    "         anchor: 'center'," +
                    "         align: 'top'," +
                    "         color: '#000000'," +
                    "         font: {" +
                    "          weight: 'bold'," +
                    "               }," +
                    "             }," +
                    "          }," +
                    "    title: {" +
                    "      display: true," +
                    "      text: 'Hangman games by month'," +
                    "    }," +
                    "  }," +
                    "}"
            );
            case MY -> chart.setConfig("{" +
                    "  type: 'line'," +
                    "  data: {" +
                    "    labels: [" + date + "]," +
                    "    datasets: [" +
                    "      {" +
                    "        label: 'Winnings'," +
                    "        backgroundColor: 'rgb(23, 114, 69)'," +
                    "        borderColor: 'rgb(23, 114, 69)'," +
                    "       " +
                    "        data: [" + columnFirst + "]," +
                    "        fill: false," +
                    "      }," +
                    "      {" +
                    "        label: 'Losses'," +
                    "        fill: false," +
                    "        backgroundColor: 'rgb(255, 99, 132)'," +
                    "        borderColor: 'rgb(255, 99, 132)'," +
                    "        data: [" + columnSecond + "]," +
                    "      }," +
                    "    ]," +
                    "  }," +
                    "  options: {" +
                    "     plugins: {" +
                    "      datalabels: {" +
                    "        anchor: 'center'," +
                    "        align: 'top'," +
                    "        color: '#000000'," +
                    "        font: {" +
                    "          weight: 'bold'," +
                    "        }," +
                    "      }," +
                    "     }," +
                    "      " +
                    "    " +
                    "    title: {" +
                    "      display: true," +
                    "      text: 'Your Hangman Statistic'," +
                    "    }," +
                    "  }," +
                    "}"
            );
        }

        return chart;
    }
}
