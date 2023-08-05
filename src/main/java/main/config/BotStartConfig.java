package main.config;

import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.hangman.Hangman;
import main.hangman.HangmanBuilder;
import main.hangman.HangmanPlayer;
import main.hangman.HangmanRegistry;
import main.jsonparser.ParserClass;
import main.model.entity.UserSettings;
import main.model.repository.HangmanGameRepository;
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
import org.boticordjava.api.io.UnsuccessfulHttpException;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

@Configuration
@EnableScheduling
public class BotStartConfig {

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

    //REPOSITORY
    private final UpdateController updateController;

    //DataBase
    @Value("${spring.datasource.url}")
    private String URL_CONNECTION;
    @Value("${spring.datasource.username}")
    private String USER_CONNECTION;
    @Value("${spring.datasource.password}")
    private String PASSWORD_CONNECTION;

    @Autowired
    public BotStartConfig(HangmanGameRepository hangmanGameRepository, UpdateController updateController) {
        idGame = hangmanGameRepository.getCountGames() == null ? 0 : hangmanGameRepository.getCountGames();
        this.updateController = updateController;
    }

    @Bean
    public void startBot() {
        try {
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().setIdGame();
            setLanguages();
            getUserSettings();

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
            e.printStackTrace();
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
            List<OptionData> options = new ArrayList<>();

            options.add(new OptionData(STRING, "game", "Setting the Game language")
                    .addChoice("english", "EN")
                    .addChoice("russian", "RU")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка игры")
            );

            options.add(new OptionData(STRING, "bot", "Setting the bot language")
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

            commands.addCommands(Commands.slash("language", "Setting language")
                    .addOptions(options)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка"));

            commands.addCommands(Commands.slash("hg", "Start the game")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Начать игру"));

            commands.addCommands(Commands.slash("stop", "Stop the game")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить игру"));

            commands.addCommands(Commands.slash("help", "Bot commands")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота"));

            commands.addCommands(Commands.slash("stats", "Get your statistics")
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
            e.printStackTrace();
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

                try {
                    BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
                    api.setBotStats(Config.getBotId(), botStats);
                } catch (UnsuccessfulHttpException un) {
                    un.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void getUserSettings() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM user_settings";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getLong("user_id_long"),
                        UserSettings.BotLanguage.valueOf(rs.getString("bot_language")));

                mapGameLanguages.put(rs.getLong("user_id_long"),
                        UserSettings.GameLanguage.valueOf(rs.getString("game_language")));

                mapGameCategory.put(rs.getLong("user_id_long"),
                        UserSettings.Category.valueOf(rs.getString("category")));
            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getUserSettings()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getAndSetActiveGames() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM active_hangman";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                HangmanRegistry instance = HangmanRegistry.getInstance();

                long userIdLong = rs.getLong("user_id_long");
                long secondUserIdLong = rs.getLong("second_user_id_long");
                String message_id_long = rs.getString("message_id_long");
                long channelIdLong = Long.parseLong(rs.getString("channel_id_long"));
                String guildIdLong = rs.getString("guild_long_id");
                String word = rs.getString("word");
                String currentHiddenWord = rs.getString("current_hidden_word");
                String guesses = rs.getString("guesses");
                int hangmanErrors = rs.getInt("hangman_errors");
                LocalDateTime game_created_time = rs.getTimestamp("game_created_time").toLocalDateTime();

                Long hangmanGuildLong = guildIdLong == null ? null : Long.valueOf(guildIdLong);

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, hangmanGuildLong, channelIdLong);

                HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                        .addHangmanPlayer(hangmanPlayer)
                        .setUpdateController(updateController)
                        .setHangmanErrors(hangmanErrors)
                        .setWord(word)
                        .setGuesses(guesses)
                        .setCurrentHiddenWord(currentHiddenWord)
                        .setLocalDateTime(game_created_time)
                        .setMessageId(Long.parseLong(message_id_long));

                if (secondUserIdLong == 0L) {
                    instance.setHangman(userIdLong, hangmanBuilder.build());
                } else {
                    HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(secondUserIdLong, hangmanGuildLong, channelIdLong);
                    hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);

                    Hangman hangman = hangmanBuilder.build();

                    instance.setHangman(userIdLong, hangman);
                    instance.setHangman(secondUserIdLong, hangman);
                }
            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getAndSetActiveGames()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
