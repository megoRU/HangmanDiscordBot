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

    public void sendStats(MessageChannel messageChannel, SlashCommandInteractionEvent event, String userAvatarUrl, String userIdLong, String name) {
        try {
            String[] statistic = gamesRepository.getStatistic(Long.valueOf(userIdLong)).replaceAll(",", " ").split(" ");

            if (statistic.length == 3) {
                //Формула:
                //Количество побед / Общее количество * Максимальное количество процентов
                if (Integer.parseInt(statistic[0]) == 0) {

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(name, null, userAvatarUrl);
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(jsonParsers.getLocale("MessageStats_Zero_Divide", userIdLong));

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