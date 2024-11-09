package main.service;

import main.config.BotStartConfig;
import main.config.Config;
import main.game.core.HangmanRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpdateStatisticsService {

    private final static Logger LOGGER = LoggerFactory.getLogger(UpdateStatisticsService.class.getName());

    public static String activity = "Preparing a cake...";

    public void update(JDA jda) {
        if (Config.isIsDev() && jda == null) return;

        try {
            HangmanRegistry instance = HangmanRegistry.getInstance();
            int competitiveQueueSize = instance.getCompetitiveQueueSize();
            int serverCount = jda.getGuilds().size();

            activity = BotStartConfig.activity + serverCount + " guilds";
            if (competitiveQueueSize == 0) {
                jda.getPresence().setActivity(Activity.playing(activity));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}