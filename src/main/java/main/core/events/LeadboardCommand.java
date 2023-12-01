package main.core.events;

import main.jsonparser.JSONParsers;
import main.model.entity.Game;
import main.model.repository.GamesRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeadboardCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final GamesRepository gamesRepository;

    @Autowired
    public LeadboardCommand(GamesRepository gamesRepository) {
        this.gamesRepository = gamesRepository;
    }

    public void board(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        event.getInteraction().deferReply().queue();
        List<Game> gameList = gamesRepository.findGamesForCurrentMonth();

        Map<Long, Integer> playerWinsMap = new HashMap<>();

        for (Game game : gameList) {
            Long playerId = game.getUserIdLong();
            boolean result = game.getResult();
            if (result) {
                playerWinsMap.merge(playerId, 1, Integer::sum);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        Map<Long, Integer> collect = playerWinsMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int count = 1;

        String gamePlayer = jsonParsers.getLocale("Game_Player", userIdLong);
        String winsLocale = jsonParsers.getLocale("wins", userIdLong);


        for (Map.Entry<Long, Integer> entry : collect.entrySet()) {
            Long playerId = entry.getKey();
            Integer wins = entry.getValue();
            stringBuilder.append(count).append(". ").append(gamePlayer).append(": <@").append(playerId).append("> | ").append(winsLocale).append(wins).append("\n");
            count++;
        }

        String leadboad = jsonParsers.getLocale("leadboad", userIdLong);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
        embedBuilder.setColor(0x00FF00);

        embedBuilder.setDescription(String.format("""
                %s
                                
                %s
                """, leadboad, stringBuilder));

        event.getHook()
                .sendMessageEmbeds(embedBuilder.build())
                .queue();
    }
}