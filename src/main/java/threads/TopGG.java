package threads;

import config.Config;
import db.DataBase;
import net.dv8tion.jda.api.entities.Activity;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;

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
                    DataBase.getConnection();
                    DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                            .token(Config.getTopGgApiToken())
                            .botId(Config.getBotId())
                            .build();

                    serverCount = BotStart.getShardManager().getGuilds().size();

                    TOP_GG_API.setStats(BotStart.getShardManager().getShards().stream().map(guild -> guild.getGuilds().size()).collect(Collectors.toList()));
                    BotStart.getShardManager().setActivity(Activity.playing(BotStart.activity
                            + TopGG.serverCount + " guilds"));

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 180000L);
    }

}