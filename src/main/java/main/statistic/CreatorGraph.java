package main.statistic;

import io.quickchart.QuickChart;
import main.core.events.StatsCommand;
import main.enums.Statistic;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.impl.StatisticGlobal;
import main.model.repository.impl.StatisticMy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreatorGraph {

    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final Logger LOGGER = Logger.getLogger(CreatorGraph.class.getName());
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
                        LocalDateTime gameDate = statisticList.get(i).getGameDate();

                        date.append(date.isEmpty() ? "" : ",").append("'").append(gameDate.toLocalDate().format(dateTimeFormatter)).append("'");
                        columnFirst.append(columnFirst.isEmpty() ? "" : ",").append("'").append(statisticList.get(i).getCount()).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }
                case MY -> {
                    List<StatisticMy> statisticList = gamesRepository.getAllMyStatistic(userIdLong);
                    for (int i = statisticList.size() - 1; i >= 0; i--) {
                        LocalDateTime gameDate = statisticList.get(i).getGameDate();

                        date.append(date.isEmpty() ? "" : ",").append("'").append(gameDate.toLocalDate().format(dateTimeFormatter)).append("'");
                        columnFirst.append(columnFirst.isEmpty() ? "" : ",").append("'").append(statisticList.get(i).getTOTAL_ONES()).append("'");
                        columnSecond.append(columnSecond.isEmpty() ? "" : ",").append("'").append(statisticList.get(i).getTOTAL_ZEROS()).append("'");
                    }
                    setImage(chart, statistic).getShortUrl();
                }
            }

            EmbedBuilder globalStats = new EmbedBuilder();
            globalStats.setColor(0x00FF00);
            globalStats.setImage(chart.getShortUrl());

            if (statistic.equals(Statistic.MY)) {
                String messageStatsYourStats = jsonParsers.getLocale("MessageStats_Your_Stats", Long.parseLong(userIdLong));
                globalStats.setTitle(messageStatsYourStats);
                StatsCommand statsCommand = new StatsCommand(gamesRepository);
                statsCommand.stats(globalStats, Long.valueOf(userIdLong));
                interactionHook.sendMessageEmbeds(globalStats.build()).queue();
            } else {
                globalStats.setTitle(jsonParsers.getLocale("MessageStats_All_Stats", Long.parseLong(userIdLong)));
                interactionHook.sendMessageEmbeds(globalStats.build()).queue();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
