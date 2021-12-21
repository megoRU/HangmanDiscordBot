package main.config;

import main.eventlisteners.*;
import main.hangman.*;
import main.jsonparser.ParserClass;
import main.model.repository.*;
import main.threads.TopGG;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Configuration
public class BotStartConfig {

    public static final String activity = "!help | ";
    private static final Integer TOTAL_SHARDS = 1;
    //String - userLongId
    public static final Map<String, String> secretCode = new HashMap<>();
    //String - guildLongId
    public static final Map<String, String> mapPrefix = new HashMap<>();
    //String - userLongId
    public static final Map<String, String> mapLanguages = new HashMap<>();
    //String - userLongId
    public static final Map<String, String> mapGameLanguages = new HashMap<>();
    private static ShardManager shardManager;
    private static int idGame;

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

    public static ShardManager getShardManager() {
        return shardManager;
    }

    @Bean
    public void startBot() throws Exception {
        //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
        HangmanRegistry.getInstance().getSetIdGame();
        //Получаем все префиксы из базы данных
        getPrefixFromDB();
//		//Получаем все языки перевода
        getLocalizationFromDB();
//		//Получаем все языки перевода для игры
        getGameLocalizationFromDB();
//		//Восстанавливаем игры активные
        getAndSetActiveGames();
//		//Устанавливаем языки
        setLanguages();

        List<GatewayIntent> intents = new ArrayList<>(
                Arrays.asList(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_EMOJIS,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_TYPING));

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(Config.getTOKEN(), intents);

        builder.disableCache(
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ACTIVITY,
                CacheFlag.MEMBER_OVERRIDES,
                CacheFlag.VOICE_STATE,
                CacheFlag.ONLINE_STATUS);

        builder.setAutoReconnect(true);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing(activity + TopGG.serverCount + " guilds"));
        builder.setBulkDeleteSplittingEnabled(false);
        builder.addEventListeners(new MessageWhenBotJoinToGuild(prefixRepository));
        builder.addEventListeners(new PrefixChange(prefixRepository));
        builder.addEventListeners(new MessageInfoHelp());
        builder.addEventListeners(new LanguageChange(languageRepository));
        builder.addEventListeners(new GameLanguageChange(gameLanguageRepository));
        builder.addEventListeners(new GameHangmanListener(hangmanGameRepository, gamesRepository, playerRepository));
        builder.addEventListeners(new MessageStats(gamesRepository));
        builder.addEventListeners(new ReactionsButton(gameLanguageRepository, hangmanGameRepository, gamesRepository, playerRepository));
        builder.addEventListeners(new DeleteAllMyData());
        builder.addEventListeners(new SlashCommand(hangmanGameRepository, gamesRepository, playerRepository));
        builder.addEventListeners(new GetGlobalStatsInGraph());
        builder.setShardsTotal(TOTAL_SHARDS);
        shardManager = builder.build();

//        Thread.sleep(15000);
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            shardManager.getShards().get(i).awaitReady();
        }

        for (int i = 0; i < BotStartConfig.getShardManager().getShards().size(); i++) {
            System.out.println("Guilds in Shard: " +
                    BotStartConfig.getShardManager().getShards().get(i).getGuildCache().size() +
                    " Shard: " + i + " " + BotStartConfig.getShardManager().getStatus(i));
        }


//        Скорее всего нужно использовать такое:

//        for (int i = 0; i < BotStart.getShardManager().getShards().size(); i++) {
//            BotStart.getShardManager().getShards().get(i).updateCommands().queue();
//        }


//        Уже не поддерживается

//        jda.awaitReady();

//        Удалить все команды

//        jda.updateCommands().queue();

//        for (int i = 0; i < BotStart.getShardManager().getShards().size(); i++) {
//            BotStart.getShardManager().getShards().get(i).getGuilds().forEach(guild -> guild.updateCommands().queue());
//        }

//        List<OptionData> options = new ArrayList<>();
//
//        options.add(new OptionData(OptionType.STRING, "game", "Setting the Game language")
//                .addChoice("eng", "eng")
//                .addChoice("rus", "rus")
//                .setRequired(true));
//
//        options.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
//                .addChoice("eng", "eng")
//                .addChoice("rus", "rus")
//                .setRequired(true));
//
//        try {
//        shardManager.getShards().forEach(g -> g.updateCommands().queue());
//
//        Thread.sleep(4000);
//
//        shardManager.getShards().forEach(g -> {
//                g.upsertCommand("language", "Setting language").addOptions(options).queue();
//                g.upsertCommand("hg-start", "Start the game").queue();
//                g.upsertCommand("hg-stop", "Stop the game").queue();
//        });
//
//        } catch (ErrorResponseException e) {
//            System.out.println("Скорее всего гильдия не дала разрешений на SlashCommands");
//        }


    }

    private void setLanguages() throws IOException, ParseException {

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
    }

    private void getPrefixFromDB() {
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

    private void getLocalizationFromDB() {
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

    private void getGameLocalizationFromDB() {
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

    private void getAndSetActiveGames() {
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
