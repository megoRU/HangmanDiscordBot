package main.threads;

import main.config.BotStartConfig;
import main.config.Config;
import net.dv8tion.jda.api.entities.Activity;
import org.discordbots.api.client.DiscordBotListAPI;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class TopGG {

    public static int serverCount;

    public void runTask() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() throws NullPointerException {
                try {
                    DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                            .token(Config.getTopGgApiToken())
                            .botId(Config.getBotId())
                            .build();
                    serverCount = BotStartConfig.jda.getGuilds().size();

                    TOP_GG_API.setStats(serverCount);

//                    TOP_GG_API.setStats(BotStartConfig.getShardManager().getShards().stream().map(guild -> guild.getGuilds().size()).collect(Collectors.toList()));
                    BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity
                            + TopGG.serverCount + " guilds"));

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 180000L);
    }

}