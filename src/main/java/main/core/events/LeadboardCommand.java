package main.core.events;

import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.impl.PlayerStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeadboardCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final GamesRepository gamesRepository;

    @Autowired
    public LeadboardCommand(GamesRepository gamesRepository) {
        this.gamesRepository = gamesRepository;
    }

    public void board(@NotNull SlashCommandInteractionEvent event) {
        long userIdLong = event.getUser().getIdLong();
        event.getInteraction().deferReply().queue();

        List<PlayerStats> gameList = gamesRepository.findStatsForCurrentMonth();

        String leadboad = jsonParsers.getLocale("leadboad", userIdLong);
        String leaderboardLose = jsonParsers.getLocale("leaderboard_lose", userIdLong);
        String wins = jsonParsers.getLocale("wins", userIdLong);
        String leaderboardGames = jsonParsers.getLocale("leaderboard_games", userIdLong);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("🏆 ").append(leadboad).append("\n\n");

        for (int i = 0; i < gameList.size(); i++) {
            PlayerStats stats = gameList.get(i);

            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> String.format("%d.", i + 1);
            };

            stringBuilder
                    .append(medal).append(" <@").append(stats.getId()).append(">\n")
                    .append("✅ ").append(wins).append(stats.getWins()).append(" | ")
                    .append("❌ ").append(leaderboardLose).append(stats.getLosses()).append("\n")
                    .append("📊 Winrate: ").append(stats.getWinrate()).append("% | ")
                    .append("🎮 ").append(leaderboardGames).append(stats.getTotalGames()).append("\n");

            // Добавляем разделитель только если это не последний игрок
            if (i != gameList.size() - 1) {
                stringBuilder.append("─────────────────────────────\n\n");
            }
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl())
                .setColor(0x00FF00)
                .setDescription(stringBuilder.toString());

        event.getHook()
                .sendMessageEmbeds(embedBuilder.build())
                .queue();
    }
}