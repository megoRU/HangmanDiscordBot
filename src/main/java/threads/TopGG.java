package threads;

import config.Config;
import db.DataBase;
import net.dv8tion.jda.api.entities.Activity;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;

public class TopGG extends Thread {

    public static int serverCount;

    @Override
    public void run() {
        try {
            while (true) {
                DataBase.getConnection();
                DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                        .token(Config.getTopGgApiToken())
                        .botId(Config.getBotId())
                        .build();
                serverCount = (int) BotStart.getJda().getGuildCache().size();
                TOP_GG_API.setStats(serverCount);
                BotStart.getJda().getPresence().setActivity(Activity.playing(BotStart.activity
                        + TopGG.serverCount + " guilds"));

                TopGG.sleep(180000);
            }
        } catch (Exception e) {
            TopGG.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}