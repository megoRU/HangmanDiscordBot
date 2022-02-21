package main.statistic;

import io.quickchart.QuickChart;
import lombok.Getter;
import main.config.BotStartConfig;
import main.eventlisteners.SenderMessage;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.impl.StatisticGlobal;
import main.model.repository.impl.StatisticMy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

@Getter
public class CreatorGraph implements SenderMessage {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final String textChannelIdLong;
    private final String userIdLong;
    private final String userName;
    private final String userAvatarUrl;
    private final SlashCommandInteractionEvent slashCommandEvent;
    private final StringBuilder date = new StringBuilder();
    private final StringBuilder columnFirst = new StringBuilder();
    private final StringBuilder columnSecond = new StringBuilder();
    private final GamesRepository gamesRepository;

    public CreatorGraph(GamesRepository gamesRepository,
                        String textChannelIdLong, String userIdLong,
                        String userName, String userUrl, SlashCommandInteractionEvent slashCommandEvent) {
        this.textChannelIdLong = textChannelIdLong;
        this.userIdLong = userIdLong;
        this.userName = userName;
        this.userAvatarUrl = userUrl;
        this.gamesRepository = gamesRepository;
        this.slashCommandEvent = slashCommandEvent;
    }

    public void createGraph(Statistic statistic) {
        try {
            QuickChart chart = new QuickChart();
            chart.setBackgroundColor("white");
            chart.setWidth(800);
            chart.setHeight(400);

            switch (statistic) {
                case GLOBAL -> {
                    List<StatisticGlobal> statisticList = gamesRepository.getAllStatistic();
                    for (int i = statisticList.size() - 1; i >= 0; i--) {
                        date.append(date.length() == 0 ? "" : ",").append("'").append(statisticList.get(i).getGameDate(), 0, 7).append("-01").append("'");
                        columnFirst.append(columnFirst.length() == 0 ? "" : ",").append("'").append(statisticList.get(i).getCount()).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }
                case MY -> {
                    List<StatisticMy> statisticList = gamesRepository.getAllMyStatistic(userIdLong);
                    for (int i = statisticList.size() - 1; i >= 0; i--) {
                        date.append(date.length() == 0 ? "" : ",").append("'").append(statisticList.get(i).getGameDate(), 0, 7).append("-01").append("'");
                        columnFirst.append(columnFirst.length() == 0 ? "" : ",").append("'").append(statisticList.get(i).getTOTAL_ONES()).append("'");
                        columnSecond.append(columnSecond.length() == 0 ? "" : ",").append("'").append(statisticList.get(i).getTOTAL_ZEROS()).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }

            }
            EmbedBuilder globalStats = new EmbedBuilder();

            if (statistic == Statistic.MY) {
                globalStats.setAuthor(userName, null, userAvatarUrl);
            }

            globalStats.setColor(0x00FF00);

            globalStats.setTitle(jsonParsers.getLocale("MessageStats_All_Stats", userIdLong));
            globalStats.setImage(chart.getShortUrl());


            if (slashCommandEvent != null) {
                sendMessage(globalStats, slashCommandEvent);
                return;
            }

            if (BotStartConfig.jda.getTextChannelById(textChannelIdLong) != null) {
                sendMessage(globalStats, BotStartConfig.jda.getTextChannelById(textChannelIdLong));
                return;
            }

            if (BotStartConfig.jda.getPrivateChannelById(textChannelIdLong) != null) {
                sendMessage(globalStats, BotStartConfig.jda.getPrivateChannelById(textChannelIdLong));
            }
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
