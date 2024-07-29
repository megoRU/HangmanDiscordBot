package main.service;

import main.config.BotStartConfig;
import main.config.Config;
import main.game.core.HangmanRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.boticordjava.api.entity.bot.stats.BotStats;
import org.boticordjava.api.impl.BotiCordAPI;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UpdateStatisticsService {

    private final static Logger LOGGER = LoggerFactory.getLogger(UpdateStatisticsService.class.getName());

    public static String activity = "Preparing a cake...";

    public void update(JDA jda) {
        if (Config.isIsDev() && jda == null) return;
        String boticord = System.getenv("BOTICORD");
        String topGgApiToken = Config.getTopGgApiToken();

        if (boticord == null || topGgApiToken == null) return;

        final DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                .token(topGgApiToken)
                .botId(Config.getBotId())
                .build();

        final BotiCordAPI api = new BotiCordAPI.Builder().token(boticord).build();

        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            int competitiveQueueSize = instance.getCompetitiveQueueSize();
            int serverCount = jda.getGuilds().size();
            TOP_GG_API.setStats(serverCount);

            activity = BotStartConfig.activity + serverCount + " guilds";
            if (competitiveQueueSize == 0) {
                jda.getPresence().setActivity(Activity.playing(activity));
            }

            AtomicInteger usersCount = new AtomicInteger();
            jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

            BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
            api.setBotStats(Config.getBotId(), botStats);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}