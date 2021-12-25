package main.config;

import main.eventlisteners.*;
import main.hangman.*;
import main.jsonparser.ParserClass;
import main.model.repository.*;
import main.threads.EngGameByTime;
import main.threads.TopGG;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Configuration
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

    @Bean
    public void startBot() {
        try {
            //Теперь HangmanRegistry знает количество игр и может отдавать правильное значение
            HangmanRegistry.getInstance().getSetIdGame();
            //Получаем все префиксы из базы данных
            getPrefixFromDB();
    		//Получаем все языки перевода
            getLocalizationFromDB();
    		//Получаем все языки перевода для игры
            getGameLocalizationFromDB();
    		//Восстанавливаем игры активные
            getAndSetActiveGames();
		    //Устанавливаем языки
            setLanguages();

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
            jdaBuilder.setActivity(Activity.playing(activity + TopGG.serverCount + " guilds"));
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

        new TopGG().runTask();
        new EngGameByTime(hangmanGameRepository).runTask();
        System.out.println("16:29");


          //Удалить все команды
        jda.updateCommands().queue();

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
        jda.getGuilds().forEach(guild -> {
            try {
                if (guild.getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS))
                guild.updateCommands().queue();
            } catch (Exception e) {
                System.out.println("В гильдии нет прав");
            }
        });

        jda.upsertCommand("language", "Setting language").addOptions(options).queue();
        jda.upsertCommand("hg", "Start the game").queue();
        jda.upsertCommand("stop", "Stop the game").queue();
        jda.upsertCommand("help", "Bot commands").queue();
        jda.upsertCommand("stats", "Get your statistics").queue();
        jda.upsertCommand("mystats", "Find out the number of your wins and losses").queue();
        jda.upsertCommand("allstats", "Find out the statistics of all the bot's games").queue();
        jda.upsertCommand("delete", "Deleting your data").queue();
    }

    private void setLanguages() {
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
}
