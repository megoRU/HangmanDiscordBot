package startbot;

import config.Config;
import db.DataBase;
import events.MessageWhenBotJoinToGuild;
import hangman.*;
import jsonparser.ParserClass;
import messagesevents.*;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import threads.TopGG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class BotStart {

    public static final String activity = "!help | ";
    private static final Integer TOTAL_SHARDS = 4;
    public static final Map<String, String> secretCode = new HashMap<>();
    public static final Map<String, String> mapPrefix = new HashMap<>();
    public static final Map<String, String> mapLanguages = new HashMap<>();
    public static final Map<String, String> mapGameLanguages = new HashMap<>();
    private static ShardManager shardManager;

    public static ShardManager getShardManager() {
        return shardManager;
    }

    public void startBot() throws Exception {
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
                        GatewayIntent.GUILD_MESSAGE_REACTIONS));

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
        builder.addEventListeners(new PrefixChange());
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

        Thread.sleep(15000);
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            shardManager.getShards().get(i).awaitReady();
        }

        for (int i = 0; i < BotStart.getShardManager().getShards().size(); i++) {
            System.out.println("Guild size: " + BotStart.getShardManager().getShards().get(i).getGuildCache().size() +
                    " Shards id " +  BotStart.getShardManager().getStatus(i));
        }


//        Скорее всего нужно использовать такое:

//        for (int i = 0; i < BotStart.getShardManager().getShards().size(); i++) {
//            BotStart.getShardManager().getShards().get(i).updateCommands().queue();
//        }


//        Уже не поддерживается

//        jda.awaitReady();

//        Удалить все команды
//        jda.updateCommands().queue();

//        List<OptionData> options = new ArrayList<>();
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
//        jda.upsertCommand("language", "Setting language").addOptions(options).queue();
//        jda.upsertCommand("hg-start", "Start the game").queue();
//        jda.upsertCommand("hg-stop", "Stop the game").queue();


//        try {
//            for (int i = 0; i < jda.getGuilds().size(); i++) {
//                System.out.println(jda.getGuilds().get(i).getName());
//
//                jda.getGuilds().get(i).upsertCommand("language", "Setting language").addOptions(options).queue();
//                jda.getGuilds().get(i).upsertCommand("hg-start", "Start the game").queue();
//                jda.getGuilds().get(i).upsertCommand("hg-stop", "Stop the game").queue();
//            }
//        } catch (Exception e) {
//        }

    }

    private void getAndSetActiveGames() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM ActiveHangman";
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

                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(String.valueOf(userIdLong), guildIdLong, Long.parseLong(channelIdLong)));
                HangmanRegistry.getInstance().getMessageId().put(userIdLong, message_id_long);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong)
                        .updateVariables(guesses, word, currentHiddenWord, hangmanErrors);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).autoInsert();

                HangmanRegistry.getInstance().getTimeCreatedGame().put(userIdLong, game_created_time);

                Instant specificTime = Instant.ofEpochMilli(game_created_time.toInstant(ZoneOffset.UTC).toEpochMilli());

                HangmanRegistry.getInstance().getEndAutoDelete().put(
                        userIdLong,
                        specificTime.plusSeconds(600L).toString());
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getPrefixFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM prefixs";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getLocalizationFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setLanguages() throws IOException, ParseException {

        List<String> listLanguages = new ArrayList<>();
        listLanguages.add("rus");
        listLanguages.add("eng");

        for (int i = 0; i < listLanguages.size(); i++) {
            InputStream inputStream = getClass().getResourceAsStream("/json/" + listLanguages.get(i) + ".json");
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

    private void getGameLocalizationFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM game_language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapGameLanguages.put(rs.getString("user_id_long"), rs.getString("language"));
            }

            rs.close();
            statement.close();
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

//    public static JDA getJda() {
//        return jda;
//    }

    public static Map<String, String> getSecretCode() {
        return secretCode;
    }
}