package main.statistic;

import io.quickchart.QuickChart;
import main.enums.Statistic;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.impl.StatisticGlobal;
import main.model.repository.impl.StatisticMy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.List;

public class CreatorGraph {

    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final GamesRepository gamesRepository;

    private final InteractionHook interactionHook;

    private final StringBuilder date = new StringBuilder();
    private final StringBuilder columnFirst = new StringBuilder();
    private final StringBuilder columnSecond = new StringBuilder();
    private final String userIdLong;

    public CreatorGraph(GamesRepository gamesRepository, String userIdLong, InteractionHook interactionHook) {
        this.userIdLong = userIdLong;
        this.gamesRepository = gamesRepository;
        this.interactionHook = interactionHook;
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

            globalStats.setColor(0x00FF00);

            globalStats.setTitle(jsonParsers.getLocale("MessageStats_All_Stats", Long.parseLong(userIdLong)));
            globalStats.setImage(chart.getShortUrl());

            interactionHook.sendMessageEmbeds(globalStats.build()).queue();
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
