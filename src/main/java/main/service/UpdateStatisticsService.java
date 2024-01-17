package main.service;

import main.config.BotStartConfig;
import main.config.Config;
import main.game.core.HangmanRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.boticordjava.api.entity.bot.stats.BotStats;
import org.boticordjava.api.impl.BotiCordAPI;
import org.discordbots.api.client.DiscordBotListAPI;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Service
public class UpdateStatisticsService {

    private static final Logger LOGGER = Logger.getLogger(UpdateStatisticsService.class.getName());

    //API
    private final DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId())
            .build();

    private final BotiCordAPI api = new BotiCordAPI.Builder()
            .token(System.getenv("BOTICORD"))
            .build();

    public void update(JDA jda) {
        if (Config.isIsDev() && jda == null) return;
        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            int competitiveQueueSize = instance.getCompetitiveQueueSize();
            int serverCount = jda.getGuilds().size();
            TOP_GG_API.setStats(serverCount);

            if (competitiveQueueSize == 0) {
                jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));
            }

            AtomicInteger usersCount = new AtomicInteger();
            jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

            BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
            api.setBotStats(Config.getBotId(), botStats);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}