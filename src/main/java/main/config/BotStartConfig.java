package main.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.model.entity.UserSettings;
import main.model.repository.HangmanGameRepository;
import main.service.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class BotStartConfig {

    private static final Logger LOGGER = Logger.getLogger(BotStartConfig.class.getName());

    public static final String activity = "/help | ";

    //String - userLongId
    @Getter
    public static final Map<Long, String> secretCode = new HashMap<>();
    @Getter
    public static final Map<Long, UserSettings.BotLanguage> mapLanguages = new HashMap<>();
    @Getter
    public static final Map<Long, UserSettings.GameLanguage> mapGameLanguages = new HashMap<>();
    @Getter
    public static final Map<Long, UserSettings.Category> mapGameCategory = new HashMap<>();

    @Getter
    private static int idGame;
    public static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //Service
    private final CompetitiveService competitiveService;
    private final UserSettingsService userSettingsService;
    private final CompetitiveQueueService competitiveQueueService;
    private final LanguageService languageService;
    private final HangmanGetService hangmanGetService;
    private final SlashService slashService;
    private final UpdateStatisticsService updateStatisticsService;

    //REPOSITORY
    private final UpdateController updateController;
    private final HangmanGameRepository hangmanGameRepository;

    @PostConstruct
    private void setLanguages() {
        try {
            languageService.language();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @PostConstruct
    private void getUserSettings() {
        try {
            userSettingsService.settings();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @PostConstruct
    private void setHangmanGetService() {
        try {
            hangmanGetService.update();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @PostConstruct
    public void startBot() {
        try {
            Integer countGames = hangmanGameRepository.getCountGames();
            idGame = countGames == null ? 0 : countGames;
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().setIdGame();

            List<GatewayIntent> intents = new ArrayList<>(
                    Arrays.asList(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGE_TYPING));

            jdaBuilder.disableCache(
                    CacheFlag.CLIENT_STATUS,
                    CacheFlag.ACTIVITY,
                    CacheFlag.MEMBER_OVERRIDES,
                    CacheFlag.VOICE_STATE,
                    CacheFlag.ONLINE_STATUS);

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
        //slashService.update(jda);
        System.out.println("15:15");
        HangmanUtils.updateActivity(jda);
    }

    @PostConstruct
    private void getCompetitiveQueue() {
        try {
            competitiveQueueService.queue(jda);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 900, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void updateStatistics() {
        try {
            updateStatisticsService.update(jda);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = (200), initialDelay = 15000)
    private void competitiveHandler() {
        try {
            competitiveService.startGame(jda);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}