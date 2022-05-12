package main.config;

import main.eventlisteners.*;
import main.eventlisteners.buttons.ButtonReactions;
import main.eventlisteners.game.GameHangmanListener;
import main.eventlisteners.slash.SlashCommand;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import main.jsonparser.ParserClass;
import main.model.repository.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.boticordjava.api.BotiCordAPI;
import org.boticordjava.api.impl.BotiCordAPIImpl;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

@Configuration
@EnableScheduling
public class BotStartConfig {

    public static final String activity = "!help | ";
    //String - userLongId
    public static final Map<String, String> secretCode = new HashMap<>();
    //String - guildLongId
    public static final Map<String, String> mapPrefix = new HashMap<>();
    //String - userLongId
    public static final Map<String, String> mapLanguages = new HashMap<>();
    //String - userLongId
    public static final Map<String, String> mapGameLanguages = new HashMap<>();
    private static int idGame;
    public static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
    private int serverCount;

    //REPOSITORY
    private final PrefixRepository prefixRepository;
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final PlayerRepository playerRepository;
    private final GamesRepository gamesRepository;

    //DataBase
    @Value("${spring.datasource.url}")
    private String URL_CONNECTION;
    @Value("${spring.datasource.username}")
    private String USER_CONNECTION;
    @Value("${spring.datasource.password}")
    private String PASSWORD_CONNECTION;

    @Autowired
    public BotStartConfig(PrefixRepository prefixRepository, LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository, HangmanGameRepository hangmanGameRepository, PlayerRepository playerRepository, GamesRepository gamesRepository) {
        this.prefixRepository = prefixRepository;
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
        this.hangmanGameRepository = hangmanGameRepository;
        this.playerRepository = playerRepository;
        this.gamesRepository = gamesRepository;
        idGame = hangmanGameRepository.getCountGames() == null ? 0 : hangmanGameRepository.getCountGames();
    }

    @Bean
    public void startBot() {
        try {
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().setIdGame();
            getPrefixFromDB();
            setLanguages();
            getLocalizationFromDB();
            getGameLocalizationFromDB();
            getAndSetActiveGames();

            List<GatewayIntent> intents = new ArrayList<>(
                    Arrays.asList(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
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
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild(prefixRepository));
            jdaBuilder.addEventListeners(new PrefixChange(prefixRepository));
            jdaBuilder.addEventListeners(new MessageInfoHelp());
            jdaBuilder.addEventListeners(new LanguageChange(languageRepository));
            jdaBuilder.addEventListeners(new GameLanguageChange(gameLanguageRepository));
            jdaBuilder.addEventListeners(new GameHangmanListener(hangmanGameRepository, gamesRepository, playerRepository));
            jdaBuilder.addEventListeners(new MessageStats(gamesRepository));
            jdaBuilder.addEventListeners(new ButtonReactions(gameLanguageRepository, languageRepository, hangmanGameRepository, gamesRepository, playerRepository));
            jdaBuilder.addEventListeners(new DeleteAllMyData(gamesRepository, languageRepository, gameLanguageRepository));
            jdaBuilder.addEventListeners(new SlashCommand(hangmanGameRepository, gamesRepository, playerRepository, gameLanguageRepository, languageRepository));
            jdaBuilder.addEventListeners(new GetGlobalStatsInGraph(gamesRepository));

            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(jda.retrieveCommands().complete());

        //Обновить команды
//        updateSlashCommands(false);
        System.out.println("19:50");
    }

    //Выглядит ужасно, но работает.
    @Scheduled(fixedDelay = 15000L, initialDelay = 15000L)
    private void engGameByTime() {
        try {
            Map<Long, LocalDateTime> timeCreatedGame = new HashMap<>(HangmanRegistry.getInstance().getTimeCreatedGame());

            for (Map.Entry<Long, LocalDateTime> entry : timeCreatedGame.entrySet()) {
                Instant instant = Instant.ofEpochMilli(Instant.now().toEpochMilli());

                if (entry.getValue().plusMinutes(10L).isBefore(ChronoLocalDateTime.from(OffsetDateTime.parse(String.valueOf(instant))))) {
                    synchronized (this) {
                        if (HangmanRegistry.getInstance().hasHangman(entry.getKey())) {
                            HangmanRegistry.getInstance().getActiveHangman().get(entry.getKey()).stopGameByTime();
                            HangmanRegistry.getInstance().getTimeCreatedGame().remove(entry.getKey());
                            hangmanGameRepository.deleteActiveGame(entry.getKey());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 140000L, initialDelay = 8000L)
    private void topGG() {
        if (!Config.isIsDev()) {
            try {
                if (Config.getTopGgApiToken() != null && System.getenv("BOTICORD") != null) {
                    DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                            .token(Config.getTopGgApiToken())
                            .botId(Config.getBotId())
                            .build();
                    serverCount = BotStartConfig.jda.getGuilds().size();
                    TOP_GG_API.setStats(serverCount);
                    BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));

                    BotiCordAPI api = new BotiCordAPIImpl(System.getenv("BOTICORD"), Config.getBotId());
                    AtomicInteger usersCount = new AtomicInteger();
                    jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));
                    api.setStats(serverCount, 1, usersCount.get());
                } else {
                    serverCount = BotStartConfig.jda.getGuilds().size();
                    BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateSlashCommands(boolean isUpdateInGuilds) {
        try {
            if (isUpdateInGuilds) {
                for (int i = 0; i < jda.getGuilds().size(); i++) {
                    jda.getGuilds().get(i).updateCommands().queue();
                }
            } else {
                CommandListUpdateAction commands = jda.updateCommands();

                List<OptionData> options = new ArrayList<>();

                options.add(new OptionData(STRING, "game", "Setting the Game language")
                        .addChoice("eng", "eng")
                        .addChoice("rus", "rus")
                        .setRequired(true));

                options.add(new OptionData(STRING, "bot", "Setting the bot language")
                        .addChoice("eng", "eng")
                        .addChoice("rus", "rus")
                        .setRequired(true));

                commands.addCommands(Commands.slash("language", "Setting language").addOptions(options));
                commands.addCommands(Commands.slash("hg", "Start the game"));
                commands.addCommands(Commands.slash("stop", "Stop the game"));
                commands.addCommands(Commands.slash("help", "Bot commands"));
                commands.addCommands(Commands.slash("stats", "Get your statistics"));
                commands.addCommands(Commands.slash("mystats", "Find out the number of your wins and losses"));
                commands.addCommands(Commands.slash("allstats", "Find out the statistics of all the bot's games"));
                commands.addCommands(Commands.slash("delete", "Deleting your data"));

                commands.queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (int i = 0; i < listLanguages.size(); i++) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguages.get(i) + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);

                for (Object o : jsonObject.keySet()) {
                    String key = (String) o;

                    if (listLanguages.get(i).equals("rus")) {
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

    private void getPrefixFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM prefixs";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                mapPrefix.put(rs.getString("server_id"), rs.getString("prefix"));
            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getPrefixFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getLocalizationFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getLocalizationFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void getGameLocalizationFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM game_language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapGameLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getGameLocalizationFromDB()");
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

                long userIdLong = rs.getLong("user_id_long");
                String message_id_long = rs.getString("message_id_long");
                String channelIdLong = rs.getString("channel_id_long");
                String guildIdLong = rs.getString("guild_long_id");
                String word = rs.getString("word");
                String currentHiddenWord = rs.getString("current_hidden_word");
                String guesses = rs.getString("guesses");
                int hangmanErrors = rs.getInt("hangman_errors");
                LocalDateTime game_created_time = rs.getTimestamp("game_created_time").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

                HangmanRegistry.getInstance().setHangman(
                        userIdLong,
                        new Hangman(String.valueOf(userIdLong), guildIdLong, Long.parseLong(channelIdLong),
                                hangmanGameRepository,
                                gamesRepository,
                                playerRepository));
                HangmanRegistry.getInstance().getMessageId().put(userIdLong, message_id_long);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong)
                        .updateVariables(guesses, word, currentHiddenWord, hangmanErrors);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).autoInsert();

                HangmanRegistry.getInstance().getTimeCreatedGame().put(userIdLong, game_created_time);

                Instant specificTime = Instant.ofEpochMilli(game_created_time.toInstant(ZoneOffset.UTC).toEpochMilli()).plusSeconds(600L);

                HangmanRegistry.getInstance().getEndAutoDelete().put(
                        userIdLong,
                        specificTime.toString());
            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getAndSetActiveGames()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getMapPrefix() {
        return mapPrefix;
    }

    public static Map<String, String> getMapLanguages() {
        return mapLanguages;
    }

    public static Map<String, String> getMapGameLanguages() {
        return mapGameLanguages;
    }

    public static Map<String, String> getSecretCode() {
        return secretCode;
    }

    public static int getIdGame() {
        return idGame;
    }
}
