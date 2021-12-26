package main.config;

import main.eventlisteners.*;
import main.hangman.*;
import main.jsonparser.ParserClass;
import main.model.repository.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;

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
            HangmanRegistry.getInstance().getSetIdGame();

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
            jdaBuilder.setActivity(Activity.playing(activity + serverCount + " guilds"));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild(prefixRepository));
            jdaBuilder.addEventListeners(new PrefixChange(prefixRepository));
            jdaBuilder.addEventListeners(new MessageInfoHelp());
            jdaBuilder.addEventListeners(new LanguageChange(languageRepository));
            jdaBuilder.addEventListeners(new GameLanguageChange(gameLanguageRepository));
            jdaBuilder.addEventListeners(new GameHangmanListener(hangmanGameRepository, gamesRepository, playerRepository));
            jdaBuilder.addEventListeners(new MessageStats(gamesRepository));
            jdaBuilder.addEventListeners(new ReactionsButton(gameLanguageRepository, languageRepository, hangmanGameRepository, gamesRepository, playerRepository));
            jdaBuilder.addEventListeners(new DeleteAllMyData(gamesRepository, languageRepository, gameLanguageRepository));
            jdaBuilder.addEventListeners(new SlashCommand(hangmanGameRepository, gamesRepository, playerRepository, gameLanguageRepository, languageRepository));
            jdaBuilder.addEventListeners(new GetGlobalStatsInGraph(gamesRepository));

            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Обновить команды
        //updateSlashCommands();
        System.out.println("14:14");
    }

    @Scheduled(fixedDelay = 15000L)
    private void engGameByTime() {
        try {
            Map<Long, LocalDateTime> timeCreatedGame = new HashMap<>(HangmanRegistry.getInstance().getTimeCreatedGame());

            for (Map.Entry<Long, LocalDateTime> entry : timeCreatedGame.entrySet()) {
                Instant specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

                if (entry.getValue().isBefore(ChronoLocalDateTime.from(OffsetDateTime.parse(String.valueOf(specificTime)).minusMinutes(10L)))) {
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

    @Scheduled(fixedDelay = 20000L)
    private void topGG() {
        try {
            DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                    .token(Config.getTopGgApiToken())
                    .botId(Config.getBotId())
                    .build();
            serverCount = BotStartConfig.jda.getGuilds().size();
            TOP_GG_API.setStats(serverCount);
            BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private void updateSlashCommands() {
        jda.updateCommands().queue();

        jda.getGuilds().forEach(guild -> System.out.println(guild.getName() + " " + guild.getSelfMember().hasPermission(Permission.USE_APPLICATION_COMMANDS)));

        List<OptionData> options = new ArrayList<>();

        options.add(new OptionData(OptionType.STRING, "game", "Setting the Game language")
                .addChoice("eng", "eng")
                .addChoice("rus", "rus")
                .setRequired(true));

        options.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
                .addChoice("eng", "eng")
                .addChoice("rus", "rus")
                .setRequired(true));

        System.out.println(jda.getGuilds().size());
        try {
            for (int i = 0; i < jda.getGuilds().size(); i++) {
                if (jda.getGuilds().get(i).getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS)) {
                    jda.getGuilds().get(i).updateCommands().queue();
                    Thread.sleep(100);
                    jda.getGuilds().get(i).upsertCommand("language", "Setting language").addOptions(options).queue();
                    jda.getGuilds().get(i).upsertCommand("hg", "Start the game").queue();
                    jda.getGuilds().get(i).upsertCommand("stop", "Stop the game").queue();
                    jda.getGuilds().get(i).upsertCommand("help", "Bot commands").queue();
                    jda.getGuilds().get(i).upsertCommand("stats", "Get your statistics").queue();
                    jda.getGuilds().get(i).upsertCommand("mystats", "Find out the number of your wins and losses").queue();
                    jda.getGuilds().get(i).upsertCommand("allstats", "Find out the statistics of all the bot's games").queue();
                    jda.getGuilds().get(i).upsertCommand("delete", "Deleting your data").queue();
                }
            }
        } catch (Exception e) {
            System.out.println("В гильдии нет прав");
        }
    }

    @Bean
    public void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (int i = 0; i < listLanguages.size(); i++) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguages.get(i) + ".json").getInputStream();


                assert inputStream != null;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void getPrefixFromDB() {
        try {
            for (int i = 0; i < prefixRepository.getPrefix().size(); i++) {
                mapPrefix.put(
                        prefixRepository.getPrefix().get(i).getServerId(),
                        prefixRepository.getPrefix().get(i).getPrefix());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void getLocalizationFromDB() {
        try {
            for (int i = 0; i < languageRepository.getLanguages().size(); i++) {
                mapLanguages.put(
                        languageRepository.getLanguages().get(i).getUserIdLong(),
                        languageRepository.getLanguages().get(i).getLanguage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void getGameLocalizationFromDB() {
        try {
            for (int i = 0; i < gameLanguageRepository.getGameLanguages().size(); i++) {
                mapGameLanguages.put(
                        gameLanguageRepository.getGameLanguages().get(i).getUserIdLong(),
                        gameLanguageRepository.getGameLanguages().get(i).getLanguage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void getAndSetActiveGames() {
        try {
            for (int i = 0; i < hangmanGameRepository.getAllActiveGames().size(); i++) {

                HangmanRegistry.getInstance().setHangman(
                        hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong(),
                        new Hangman(String.valueOf(hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong()),
                                String.valueOf(hangmanGameRepository.getAllActiveGames().get(i).getGuildLongId()),
                                hangmanGameRepository.getAllActiveGames().get(i).getChannelIdLong(),
                                hangmanGameRepository,
                                gamesRepository,
                                playerRepository));

                HangmanRegistry.getInstance().getMessageId().put(
                        hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong(),
                        String.valueOf(hangmanGameRepository.getAllActiveGames().get(i).getMessageIdLong()));


                HangmanRegistry.getInstance().getActiveHangman().get(
                                hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong())
                        .updateVariables(
                                hangmanGameRepository.getAllActiveGames().get(i).getGuesses(),
                                hangmanGameRepository.getAllActiveGames().get(i).getWord(),
                                hangmanGameRepository.getAllActiveGames().get(i).getCurrentHiddenWord(),
                                hangmanGameRepository.getAllActiveGames().get(i).getHangmanErrors());

                HangmanRegistry.getInstance().getActiveHangman().get(hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong()).autoInsert();


                LocalDateTime game_created_time = hangmanGameRepository.getAllActiveGames().get(i).getGameCreatedTime().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();


                HangmanRegistry.getInstance().getTimeCreatedGame().put(
                        hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong(), game_created_time);


                Instant specificTime = Instant.ofEpochMilli(game_created_time.toInstant(ZoneOffset.UTC).toEpochMilli());

                HangmanRegistry.getInstance().getEndAutoDelete().put(
                        hangmanGameRepository.getAllActiveGames().get(i).getUserIdLong(),
                        specificTime.plusSeconds(600L).toString());
            }
        } catch (Exception e) {
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
