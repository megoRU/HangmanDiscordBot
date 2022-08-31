package main.eventlisteners.buildClass;

import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@AllArgsConstructor
@Service
public class MessageStats {

    private static final DecimalFormat df = new DecimalFormat("##.#");
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private GamesRepository gamesRepository;

    public void sendStats(MessageChannel messageChannel, SlashCommandInteractionEvent event, String userAvatarUrl, long userIdLong, String name) {
        try {
            String[] statistic = gamesRepository.getStatistic(userIdLong).replaceAll(",", " ").split(" ");

            if (statistic.length == 3) {
                //Формула:
                //Количество побед / Общее количество * Максимальное количество процентов
                if (Integer.parseInt(statistic[0]) == 0) {
                    String messageStatsZeroDivide = jsonParsers.getLocale("MessageStats_Zero_Divide", userIdLong);

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(name, null, userAvatarUrl);
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(messageStatsZeroDivide);

                    if (messageChannel != null) {
                        messageChannel.sendMessageEmbeds(needSetLanguage.build()).queue();
                    } else {
                        event.replyEmbeds(needSetLanguage.build()).queue();
                    }
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
                stats.setAuthor(name, null, avatarUrl);
                stats.setTitle(messageStatsYourStats);
                stats.setDescription(messageStatsGameCount + messageStatsGameWins + messageStatsGamePercentage);

                if (messageChannel != null) {
                    messageChannel.sendMessageEmbeds(stats.build()).queue();
                } else {
                    event.replyEmbeds(stats.build()).queue();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}