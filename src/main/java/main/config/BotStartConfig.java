package main.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.jsonparser.ParserClass;
import main.model.entity.ActiveHangman;
import main.model.entity.CompetitiveQueue;
import main.model.entity.UserSettings;
import main.model.repository.CompetitiveQueueRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.UserSettingsRepository;
import main.service.CompetitiveService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.boticordjava.api.entity.bot.stats.BotStats;
import org.boticordjava.api.impl.BotiCordAPI;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

@Configuration
@EnableScheduling
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

    //API
    private final DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId())
            .build();

    private final BotiCordAPI api = new BotiCordAPI.Builder()
            .token(System.getenv("BOTICORD"))
            .build();

    private final CompetitiveService competitiveService;
    private final HangmanDataSaving hangmanDataSaving;

    //REPOSITORY
    private final UpdateController updateController;
    private final UserSettingsRepository userSettingsRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final CompetitiveQueueRepository competitiveQueueRepository;

    //Service
    private final HangmanResult hangmanResult;

    @Autowired
    public BotStartConfig(HangmanGameRepository hangmanGameRepository,
                          CompetitiveService competitiveService,
                          HangmanDataSaving hangmanDataSaving,
                          UpdateController updateController,
                          UserSettingsRepository userSettingsRepository,
                          CompetitiveQueueRepository competitiveQueueRepository,
                          HangmanResult hangmanResult) {
        idGame = hangmanGameRepository.getCountGames() == null ? 0 : hangmanGameRepository.getCountGames();
        this.competitiveService = competitiveService;
        this.hangmanDataSaving = hangmanDataSaving;
        this.updateController = updateController;
        this.userSettingsRepository = userSettingsRepository;
        this.hangmanGameRepository = hangmanGameRepository;
        this.competitiveQueueRepository = competitiveQueueRepository;
        this.hangmanResult = hangmanResult;
    }

    @PostConstruct
    public void startBot() {
        try {
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().setIdGame();
            setLanguages();
            getUserSettings();
            getCompetitiveQueue();

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
        getAndSetActiveGames();

        List<Command> complete = jda.retrieveCommands().complete();
        complete.forEach(command -> System.out.println(command.toString()));

        System.out.println("IsDevMode: " + Config.isIsDev());

        //Обновить команды
//        updateSlashCommands();
        System.out.println("18:31");
    }

    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();
            List<OptionData> language = new ArrayList<>();

            language.add(new OptionData(STRING, "game", "Setting the Game language")
                    .addChoice("english", "EN")
                    .addChoice("russian", "RU")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка игры")
            );

            language.add(new OptionData(STRING, "bot", "Setting the bot language")
                    .addChoice("english", "EN")
                    .addChoice("russian", "RU")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота")
            );

            List<OptionData> multi = new ArrayList<>();
            multi.add(new OptionData(USER, "user", "@Mention player to play with him")
                    .setRequired(true)
                    .setName("user")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "@Упомяните игрока, чтобы поиграть с ним")
            );

            List<OptionData> category = new ArrayList<>();
            category.add(new OptionData(STRING, "category", "Select a category")
                    .addChoice("any", "any")
                    .addChoice("colors", "COLORS")
                    .addChoice("fruits", "FRUITS")
                    .addChoice("flowers", "FLOWERS")
                    .setRequired(true)
                    .setName("category")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выбрать категорию")
            );

            commands.addCommands(Commands.slash("competitive", "Compete with other players")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Соревноваться с другими игроками"));

            commands.addCommands(Commands.slash("language", "Setting language")
                    .addOptions(language)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка"));

            commands.addCommands(Commands.slash("hg", "Start the game (deprecated: use /play)")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Начать игру (Устарело: используй /play)"));

            commands.addCommands(Commands.slash("play", "Play Hangman")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играть в Виселицу"));

            commands.addCommands(Commands.slash("stop", "Stop the game (deprecated: use /quit)")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить игру (Устарело: используй /quit)"));

            commands.addCommands(Commands.slash("quit", "Leave from singleplayer/multiplayer game")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выход из одиночной/многопользовательской игры"));

            commands.addCommands(Commands.slash("help", "Bot commands")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота"));

            commands.addCommands(Commands.slash("leadboard", "Leadboard")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Доска почёта"));

            commands.addCommands(Commands.slash("statistics", "Get your statistics")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить свою статистику"));

            commands.addCommands(Commands.slash("mystats", "Find out the number of your wins and losses")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Узнайте количество ваших побед и поражений"));

            commands.addCommands(Commands.slash("allstats", "Find out the statistics of all the bot games")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Узнайте статистику всех игр бота"));

            commands.addCommands(Commands.slash("delete", "Deleting your data")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удаление ваших данных"));

            commands.addCommands(Commands.slash("multi", "Play Hangman with another player")
                    .setGuildOnly(true)
                    .addOptions(multi)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играйте в Hangman с другим игроком"));

            //Context Menu
            commands.addCommands(Commands.context(Command.Type.USER, "Play multi")
                    .setName("multi")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "Играть вместе")
                    .setGuildOnly(true)
            );

            commands.addCommands(Commands.slash("category", "Set a category for words")
                    .addOptions(category)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите категорию для слов"));

            commands.queue();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 900000L, initialDelay = 8000L)
    private void topGG() {
        if (!Config.isIsDev()) {
            try {
                int serverCount = BotStartConfig.jda.getGuilds().size();
                TOP_GG_API.setStats(serverCount);
                BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));

                AtomicInteger usersCount = new AtomicInteger();
                jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

                BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
                api.setBotStats(Config.getBotId(), botStats);
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);

                for (Object o : jsonObject.keySet()) {
                    String key = (String) o;

                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(key, String.valueOf(jsonObject.get(key)));
                    } else {
                        ParserClass.english.put(key, String.valueOf(jsonObject.get(key)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void getUserSettings() {
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();

        for (UserSettings userSettings : userSettingsList) {
            Long userIdLong = userSettings.getUserIdLong();
            UserSettings.BotLanguage botLanguage = userSettings.getBotLanguage();
            UserSettings.GameLanguage gameLanguage = userSettings.getGameLanguage();
            UserSettings.Category category = userSettings.getCategory();

            mapLanguages.put(userIdLong, botLanguage);
            mapGameLanguages.put(userIdLong, gameLanguage);
            mapGameCategory.put(userIdLong, category);
        }
        System.out.println("getUserSettings()");
    }

    public void getCompetitiveQueue() {
        List<CompetitiveQueue> competitiveQueueList = competitiveQueueRepository.findAll();
        HangmanRegistry instance = HangmanRegistry.getInstance();

        for (CompetitiveQueue competitiveQueue : competitiveQueueList) {
            Long userIdLong = competitiveQueue.getUserIdLong();
            Long messageChannel = competitiveQueue.getMessageChannel();
            UserSettings.GameLanguage gameLanguage = competitiveQueue.getGameLanguage();
            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, messageChannel, gameLanguage);
            instance.addCompetitiveQueue(hangmanPlayer);
        }
        System.out.println("getCompetitiveQueue()");
    }

    private void getAndSetActiveGames() {
        List<ActiveHangman> activeHangmanList = hangmanGameRepository.findAll();
        HangmanRegistry instance = HangmanRegistry.getInstance();

        for (ActiveHangman activeHangman : activeHangmanList) {
            Long userIdLong = activeHangman.getUserIdLong();
            Long secondUserIdLong = activeHangman.getSecondUserIdLong();
            Long messageIdLong = activeHangman.getMessageIdLong();
            Long channelIdLong = activeHangman.getChannelIdLong();
            Long guildLongId = activeHangman.getGuildLongId();
            String word = activeHangman.getWord();
            String currentHiddenWord = activeHangman.getCurrentHiddenWord();
            String guesses = activeHangman.getGuesses();
            Integer hangmanErrors = activeHangman.getHangmanErrors();
            LocalDateTime gameCreatedTime = activeHangman.getGameCreatedTime().toLocalDateTime();
            Boolean isCompetitive = activeHangman.getIsCompetitive();
            Long againstPlayerId = activeHangman.getAgainstPlayerId();

            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guildLongId, channelIdLong);

            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                    .setHangmanGameRepository(hangmanGameRepository)
                    .setHangmanDataSaving(hangmanDataSaving)
                    .setHangmanResult(hangmanResult)
                    .addHangmanPlayer(hangmanPlayer)
                    .setHangmanErrors(hangmanErrors)
                    .setWord(word)
                    .setGuesses(guesses)
                    .setCurrentHiddenWord(currentHiddenWord)
                    .setLocalDateTime(gameCreatedTime)
                    .setCompetitive(isCompetitive)
                    .setAgainstPlayerId(againstPlayerId)
                    .setMessageId(messageIdLong);

            if (secondUserIdLong == null) {
                instance.setHangman(userIdLong, hangmanBuilder.build());
            } else {
                HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(secondUserIdLong, guildLongId, channelIdLong);
                hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);

                Hangman hangman = hangmanBuilder.build();

                instance.setHangman(userIdLong, hangman);
                instance.setHangman(secondUserIdLong, hangman);
            }
        }
        System.out.println("getAndSetActiveGames()");
    }

    @Scheduled(fixedDelay = (200), initialDelay = 1000)
    public void competitiveHandler() {
        try {
            competitiveService.startGame();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}