package startbot;

import config.Config;
import db.DataBase;
import events.MessageWhenBotJoinToGuild;
import hangman.*;
import lombok.Getter;
import messagesevents.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import threads.TopGG;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class BotStart {

    public static final String activity = "!help | ";
    @Getter
    private static final Map<String, String> secretCode = new HashMap<>();
    @Getter
    private static final Map<String, String> mapPrefix = new HashMap<>();
    @Getter
    private static final Map<String, String> mapLanguages = new HashMap<>();
    @Getter
    private static final Map<String, String> mapGameLanguages = new HashMap<>();
    @Getter
    private static JDA jda;

    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    public void startBot() throws Exception {
        //Теперь HangmanRegistry знает колличство игр и может отдавать правильное значение
        HangmanRegistry.getInstance().getSetIdGame();
        //Получаем все префиксы из базы данных
        getPrefixFromDB();
        //Получаем все языки перевода
        getLocalizationFromDB();
        //Получаем все языки перевода для игры
        getGameLocalizationFromDB();
        //Восстанавливаем игры активные
        getAndSetActiveGames();

        jdaBuilder.setAutoReconnect(true);
        jdaBuilder.setStatus(OnlineStatus.ONLINE);
        jdaBuilder.setActivity(Activity.playing(activity + TopGG.serverCount + " guilds"));
        jdaBuilder.setBulkDeleteSplittingEnabled(false);
        jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
        jdaBuilder.addEventListeners(new PrefixChange());
        jdaBuilder.addEventListeners(new MessageInfoHelp());
        jdaBuilder.addEventListeners(new LanguageChange());
        jdaBuilder.addEventListeners(new GameLanguageChange());
        jdaBuilder.addEventListeners(new GameHangmanListener());
        jdaBuilder.addEventListeners(new MessageStats());
        jdaBuilder.addEventListeners(new MessageGlobalStats());
        jdaBuilder.addEventListeners(new ReactionsButton());
        jdaBuilder.addEventListeners(new DeleteAllMyData());
        jdaBuilder.addEventListeners(new SlashCommand());

        jda = jdaBuilder.build();
        jda.awaitReady();

        //Slash
//        CommandListUpdateAction commands = jda.updateCommands();
//        commands.addCommands(
//                new CommandData("hg-start", "Start the game"),
//                new CommandData("hg-stop", "Stop the game")
//        );
////        commands.queue();



//        Как я понял заменяет все старые на эти команды
//        jda.updateCommands().addCommands(
//                        new CommandData("hg-start", "Start the game"),
//                        new CommandData("hg-stop", "Stop the game"))
//                .queue();



//        Обновляет или создает

//        jda.upsertCommand("hg-start", "Start the game").queue();
//        jda.upsertCommand("hg-stop", "Stop the game").queue();

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

                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(String.valueOf(userIdLong), guildIdLong, Long.parseLong(channelIdLong)));
                HangmanRegistry.getInstance().getMessageId().put(userIdLong, message_id_long);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong)
                        .updateVariables(guesses, word, currentHiddenWord, hangmanErrors);

                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).autoInsert();
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

}