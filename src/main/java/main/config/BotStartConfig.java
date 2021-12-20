package main.config;

import main.eventlisteners.MessageWhenBotJoinToGuild;
import main.hangman.GameHangmanListener;
import main.hangman.ReactionsButton;
import main.hangman.SlashCommand;
import main.jsonparser.ParserClass;
import main.eventlisteners.*;
import main.model.repository.GameLanguageRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.PrefixRepository;
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

import java.io.*;
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

    //REPOSITORY
    private final PrefixRepository prefixRepository;
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;

    @Autowired
    public BotStartConfig(PrefixRepository prefixRepository, LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository) {
        this.prefixRepository = prefixRepository;
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
    }

    public static ShardManager getShardManager() {
        return shardManager;
    }

    @Bean
    public void startBot() throws Exception {
        //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
//		HangmanRegistry.getInstance().getSetIdGame();
        //Получаем все префиксы из базы данных
//		getPrefixFromDB();
//		//Получаем все языки перевода
//		getLocalizationFromDB();
//		//Получаем все языки перевода для игры
//		getGameLocalizationFromDB();
//		//Восстанавливаем игры активные
//		getAndSetActiveGames();
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
        builder.addEventListeners(new MessageWhenBotJoinToGuild());
        builder.addEventListeners(new PrefixChange(prefixRepository));
        builder.addEventListeners(new MessageInfoHelp());
        builder.addEventListeners(new LanguageChange());
        builder.addEventListeners(new GameLanguageChange());
        builder.addEventListeners(new GameHangmanListener());
        builder.addEventListeners(new MessageStats());
        builder.addEventListeners(new ReactionsButton());
        builder.addEventListeners(new DeleteAllMyData());
        builder.addEventListeners(new SlashCommand());
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
}
