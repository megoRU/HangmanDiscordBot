package main.config;

import main.eventlisteners.DeleteAllMyData;
import main.eventlisteners.GameHangmanListener;
import main.eventlisteners.MessageWhenBotJoinToGuild;
import main.eventlisteners.buttons.ButtonReactions;
import main.eventlisteners.selections.SelectMenuEvent;
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
import org.boticordjava.api.entity.Enums.TokenEnum;
import org.boticordjava.api.impl.BotiCordAPI;
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
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

@Configuration
@EnableScheduling
public class BotStartConfig {

    public static final String activity = "/help | ";

    //String - userLongId
    public static final Map<Long, String> secretCode = new HashMap<>();
    public static final Map<Long, String> mapLanguages = new HashMap<>();
    public static final Map<Long, String> mapGameLanguages = new HashMap<>();
    public static final Map<Long, String> mapGameMode = new HashMap<>();

    private static int idGame;
    public static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //REPOSITORY
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final PlayerRepository playerRepository;
    private final GamesRepository gamesRepository;
    private final GameModeRepository gameModeRepository;

    //DataBase
    @Value("${spring.datasource.url}")
    private String URL_CONNECTION;
    @Value("${spring.datasource.username}")
    private String USER_CONNECTION;
    @Value("${spring.datasource.password}")
    private String PASSWORD_CONNECTION;

    @Autowired
    public BotStartConfig(LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository,
                          HangmanGameRepository hangmanGameRepository, PlayerRepository playerRepository,
                          GamesRepository gamesRepository, GameModeRepository gameModeRepository) {
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
        this.hangmanGameRepository = hangmanGameRepository;
        this.playerRepository = playerRepository;
        this.gamesRepository = gamesRepository;
        this.gameModeRepository = gameModeRepository;
        idGame = hangmanGameRepository.getCountGames() == null ? 0 : hangmanGameRepository.getCountGames();
    }

    @Bean
    public void startBot() {
        try {
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().setIdGame();
            setLanguages();
            getGameModeFromDB();
            getLocalizationFromDB();
            getGameLocalizationFromDB();


            List<GatewayIntent> intents = new ArrayList<>(
                    Arrays.asList(
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
            jdaBuilder.addEventListeners(new GameHangmanListener());
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
            jdaBuilder.addEventListeners(new SelectMenuEvent());
            jdaBuilder.addEventListeners(new ButtonReactions(gameLanguageRepository, languageRepository, hangmanGameRepository, gamesRepository, playerRepository, gameModeRepository));
            jdaBuilder.addEventListeners(new DeleteAllMyData(gamesRepository, languageRepository, gameLanguageRepository, gameModeRepository));
            jdaBuilder.addEventListeners(new SlashCommand(hangmanGameRepository, gamesRepository, playerRepository, gameLanguageRepository, languageRepository, gameModeRepository));

            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getAndSetActiveGames();

        System.out.println(jda.retrieveCommands().complete());

        //Обновить команды
        updateSlashCommands();
        System.out.println("20:11");
    }

    private void updateSlashCommands() {
        try {
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

            List<OptionData> word = new ArrayList<>();
            word.add(new OptionData(STRING, "guess", "Write a word that can be")
                    .setRequired(true)
                    .setName("guess"));

            List<OptionData> mode = new ArrayList<>();
            mode.add(new OptionData(STRING, "mode", "Using different interaction logics.")
                    .addChoice("direct-message", "direct-message").setDescription("The game is only in DM. Write one letter to the chat.")
                    .addChoice("select-menu", "select-menu").setDescription("Playing in Guild/DM. Using SelectMenu.")
                    .setRequired(true)
                    .setName("mode"));

            commands.addCommands(Commands.slash("language", "Setting language").addOptions(options));
            commands.addCommands(Commands.slash("hg", "Start the game"));
            commands.addCommands(Commands.slash("stop", "Stop the game"));
            commands.addCommands(Commands.slash("help", "Bot commands"));
            commands.addCommands(Commands.slash("stats", "Get your statistics"));
            commands.addCommands(Commands.slash("mystats", "Find out the number of your wins and losses"));
            commands.addCommands(Commands.slash("allstats", "Find out the statistics of all the bot's games"));
            commands.addCommands(Commands.slash("delete", "Deleting your data"));
            commands.addCommands(Commands.slash("word", "Guess the full word").addOptions(word));
            commands.addCommands(Commands.slash("set-game", "Using different interaction logics").addOptions(mode));


            commands.queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 900000L, initialDelay = 8000L)
    private void topGG() {
        if (!Config.isIsDev()) {
            try {
                DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                        .token(Config.getTopGgApiToken())
                        .botId(Config.getBotId())
                        .build();
                int serverCount = BotStartConfig.jda.getGuilds().size();
                TOP_GG_API.setStats(serverCount);
                BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));

                BotiCordAPI api = new BotiCordAPI.Builder()
                        .tokenEnum(TokenEnum.BOT)
                        .token(System.getenv("BOTICORD"))
                        .build();

                AtomicInteger usersCount = new AtomicInteger();
                jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));
                api.setStats(serverCount, 1, usersCount.get());
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

    private void getLocalizationFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getLong("user_id_long"), rs.getString("language"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getLocalizationFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getGameModeFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM game_mode";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapGameMode.put(rs.getLong("user_id_long"), rs.getString("mode"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getGameModeFromDB()");
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
                mapGameLanguages.put(Long.valueOf(rs.getString("user_id_long")), rs.getString("language"));
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

                System.out.println(game_created_time);

                Hangman hangman = new Hangman(
                        userIdLong,
                        guildIdLong == null ? null : Long.valueOf(guildIdLong),
                        Long.parseLong(channelIdLong),
                        hangmanGameRepository,
                        gamesRepository,
                        playerRepository);

                HangmanRegistry.getInstance().setHangman(userIdLong, hangman);

                HangmanRegistry.getInstance().getMessageId().put(userIdLong, message_id_long);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong)
                        .updateVariables(guesses, word, currentHiddenWord, hangmanErrors, game_created_time);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).autoInsert();

                HangmanRegistry.getInstance().getTimeCreatedGame().put(userIdLong, game_created_time);

            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getAndSetActiveGames()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<Long, String> getMapLanguages() {
        return mapLanguages;
    }

    public static Map<Long, String> getMapGameLanguages() {
        return mapGameLanguages;
    }

    public static Map<Long, String> getMapGameMode() {
        return mapGameMode;
    }

    public static Map<Long, String> getSecretCode() {
        return secretCode;
    }

    public static int getIdGame() {
        return idGame;
    }
}
