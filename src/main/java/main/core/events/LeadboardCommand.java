package main.core.events;

import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.impl.PlayerWins;
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
        var userIdLong = event.getUser().getIdLong();
        event.getInteraction().deferReply().queue();
        List<PlayerWins> gameList = gamesRepository.findGamesForCurrentMonth();

        StringBuilder stringBuilder = new StringBuilder();

        String gamePlayer = jsonParsers.getLocale("Game_Player", userIdLong);
        String winsLocale = jsonParsers.getLocale("wins", userIdLong);


        for (int i = 0; i < gameList.size(); i++) {
            PlayerWins playerWins = gameList.get(i);
            Long wins = playerWins.getWins();
            Long playerId = playerWins.getId();

            stringBuilder
                    .append(i + 1)
                    .append(". ")
                    .append(gamePlayer)
                    .append(": <@")
                    .append(playerId)
                    .append("> | ")
                    .append(winsLocale)
                    .append(wins)
                    .append("\n");
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