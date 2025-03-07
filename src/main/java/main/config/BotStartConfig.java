package main.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import main.service.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableScheduling
@AllArgsConstructor
public class BotStartConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotStartConfig.class.getName());

    public static final String activity = "/play | ";

    //String - userLongId
    @Getter
    public static final Map<Long, String> secretCode = new HashMap<>();

    @Getter
    public static final Map<Long, UserSettings> userSettingsMap = new HashMap<>();

    public static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //Service
    private final CompetitiveService competitiveService;
    private final UserSettingsService userSettingsService;
    private final CompetitiveQueueService competitiveQueueService;
    private final LanguageService languageService;
    private final HangmanGetService hangmanGetService;
    private final SlashService slashService;
    private final CompetitiveGames competitiveGames;
    private final UpdateStatisticsService updateStatisticsService;
    private final ChatGPTService chatGPTService;

    //REPOSITORY
    private final UpdateController updateController;

    @PostConstruct
    private void setLanguages() {
        try {
            languageService.language();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    private void setCompetitive() {
        try {
            competitiveGames.update();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    private void getUserSettings() {
        try {
            userSettingsService.settings();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    private void setHangmanGetService() {
        try {
            hangmanGetService.update();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    public void startBot() {
        try {
            CoreBot coreBot = new CoreBot(updateController);
            coreBot.init();

            List<GatewayIntent> intents = new ArrayList<>(
                    Arrays.asList(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGE_TYPING));

            jdaBuilder.disableCache(
                    CacheFlag.ACTIVITY,
                    CacheFlag.VOICE_STATE,
                    CacheFlag.EMOJI,
                    CacheFlag.STICKER,
                    CacheFlag.CLIENT_STATUS,
                    CacheFlag.MEMBER_OVERRIDES,
                    CacheFlag.ROLE_TAGS,
                    CacheFlag.FORUM_TAGS,
                    CacheFlag.ONLINE_STATUS,
                    CacheFlag.SCHEDULED_EVENTS
            );

            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.enableIntents(intents);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new CoreBot(updateController));
            jda = jdaBuilder.build();

            jda.awaitReady();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }

        List<Command> complete = jda.retrieveCommands().complete();
        complete.forEach(command -> System.out.println(command.toString()));

        System.out.println("IsDevMode: " + Config.isIsDev());

        //Обновить команды
        slashService.updateSlash(jda);
        System.out.println("17:37");
        HangmanUtils.updateActivity(jda);
    }

    @Scheduled(fixedDelay = 2, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    private void getCompetitiveQueue() {
        try {
            competitiveQueueService.queue(jda);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    private void setChatGPTService() {
        try {
            chatGPTService.request();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 900, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void updateStatistics() {
        try {
            updateStatisticsService.update(jda);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = (1000), initialDelay = 15000)
    private void competitiveHandler() {
        try {
            competitiveService.startGame(jda);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}