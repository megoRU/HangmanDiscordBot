package threads;

import config.Config;
import db.DataBase;
import net.dv8tion.jda.api.entities.Activity;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;

import java.util.Timer;
import java.util.TimerTask;

public class TopGG {

    public static int serverCount;

    public void runTask() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() throws NullPointerException {
                try {
                    System.out.println("Начал");
                    DataBase.getConnection();
                    DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                            .token(Config.getTopGgApiToken())
                            .botId(Config.getBotId())
                            .build();
                    serverCount = (int) BotStart.getJda().getGuildCache().size();
                    TOP_GG_API.setStats(serverCount);
                    BotStart.getJda().getPresence().setActivity(Activity.playing(BotStart.activity
                            + TopGG.serverCount + " guilds"));
                    System.out.println("закончил");

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 180000L);
    }

}