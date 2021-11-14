package db;

import config.Config;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DataBase {

    private static volatile DataBase dataBase;

    private DataBase() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                    Config.getHangmanConnection(),
                    Config.getHangmanUser(),
                    Config.getHangmanPass());
    }

    public static DataBase getInstance() {
        if (dataBase == null) {
            synchronized (DataBase.class) {
                if (dataBase == null) {
                    dataBase = new DataBase();
                }
            }
        }
        return dataBase;
    }

    public void deleteAllMyData(String userIdLong) {
        try {
            String sql = "DELETE g FROM DiscordBotHangmanDEV.games g " +
                    "JOIN player p on g.id = p.games_id " +
                    "WHERE p.user_id_long ='" + userIdLong + "'";
            getConnection().prepareStatement(sql).execute();

            //Удаляем так же языки
            removeGameLanguageFromDB(userIdLong);
            removeLanguageFromDB(userIdLong);
            deleteActiveGame(userIdLong);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteActiveGame(String userIdLong) {
        try {
            String sql = "DELETE FROM ActiveHangman WHERE user_id_long = '" + userIdLong + "'";
            getConnection().prepareStatement(sql).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createGame(String userId, String messageIdLong, String channelId, String guildId,
                           String WORD, String currentHiddenWord,
                           String guesses, String hangmanErrors) {
        try {
            ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);

            String sql = "REPLACE INTO ActiveHangman " +
                    "(user_id_long, message_id_long, channel_id_long, " +
                    "guild_long_id, word, current_hidden_word, " +
                    "guesses, hangman_errors, game_created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, messageIdLong);
            preparedStatement.setString(3, channelId);
            preparedStatement.setString(4, guildId);
            preparedStatement.setString(5, WORD);
            preparedStatement.setString(6, currentHiddenWord);
            preparedStatement.setString(7, guesses);
            preparedStatement.setString(8, hangmanErrors);
            preparedStatement.setTimestamp(9, new Timestamp(Timestamp.valueOf(now.toLocalDateTime()).getTime()));
            preparedStatement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateGame(String userId, String currentHiddenWord, String guesses, String hangmanErrors) {
        try {
            String sql = "UPDATE ActiveHangman SET current_hidden_word='" +
                    currentHiddenWord + "', guesses= '" +
                    guesses + "', hangman_errors= '" + hangmanErrors + "' WHERE user_id_long= '" + userId + "'";
            getConnection().prepareStatement(sql).execute();
        } catch (SQLException e) {
            System.out.println("Скорее всего игра уже закончилась!");
        }
    }

    //Добавление префикса
    public void addPrefixToDB(String serverId, String prefix) {
        try {
            String sql = "INSERT INTO prefixs (serverId, prefix) VALUES (?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, serverId);
            preparedStatement.setString(2, prefix);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Удаление префикса
    public void removePrefixFromDB(String serverId) throws SQLException {
        String sql = "DELETE FROM prefixs WHERE serverId='" + serverId + "'";
        getConnection().prepareStatement(sql).execute();
    }

    //Добавляем в Бд данные о результате игры
    public void addResultGame(long id, boolean result) {
        try {
            ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);
            Timestamp timestamp = Timestamp.valueOf(now.toLocalDateTime());
            String sql = "INSERT IGNORE INTO games (id, result, game_date) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setLong(1, id);
            preparedStatement.setBoolean(2, result);
            preparedStatement.setTimestamp(3, new Timestamp(timestamp.getTime()));
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Добавляем связь id игры с игроком
    public void addResultPlayer(long userIdLong, int games_id) {
        try {
            String sql = "INSERT IGNORE INTO player (user_id_long, games_id) VALUES (?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setLong(1, userIdLong);
            preparedStatement.setInt(2, games_id);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Добавление языка
    public void addLanguageToDB(String userIdLong, String language) {
        try {
            String sql = "REPLACE INTO language (user_id_long, language) VALUES (?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, userIdLong);
            preparedStatement.setString(2, language);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Удаление языка
    public void removeLanguageFromDB(String userIdLong) throws SQLException {
        String sql = "DELETE FROM language WHERE user_id_long='" + userIdLong + "'";
        getConnection().prepareStatement(sql).execute();
    }

    //Добавление языка игры
    public void addGameLanguageToDB(String userIdLong, String language) {
        try {
            String sql = "REPLACE INTO game_language (user_id_long, language) VALUES (?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, userIdLong);
            preparedStatement.setString(2, language);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Удаление языка игры
    public void removeGameLanguageFromDB(String userIdLong) throws SQLException {
        String sql = "DELETE FROM game_language WHERE user_id_long='" + userIdLong + "'";
        getConnection().prepareStatement(sql).execute();
    }

    public String getStatistic(String userIdLong) throws NullPointerException {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT COUNT(games_id) AS COUNT_GAMES, " +
                    "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
                    "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES " +
                    "FROM player, games WHERE player.user_id_long = '" + userIdLong + "' " +
                    "AND player.games_id = games.id";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                return rs.getString("COUNT_GAMES") + " " +
                        rs.getString("TOTAL_ZEROS") + " " +
                        rs.getString("TOTAL_ONES");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new NullPointerException();
    }

    /**
     * Получаем максимальное число (ID)
     **/
    public synchronized int getCountGames() {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MAX(id) AS id FROM games");
            if (resultSet.next()) {
                System.out.println(resultSet.getInt(1));
                return resultSet.getInt(1);
            }
            statement.close();
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public ResultSet getAllStatistic() throws SQLException {
        Statement statement = DataBase.getConnection().createStatement();
        String sql = "SELECT COUNT(*) AS count, game_date FROM games GROUP BY MONTH (game_date);";

        if (statement.executeQuery(sql) == null) {
            getAllStatistic();
        }

        return statement.executeQuery(sql);
    }

    public ResultSet getMyAllStatistic(String userIdLong) throws SQLException {

        Statement statement = DataBase.getConnection().createStatement();
        String sql = "SELECT SUM(IF(result = 0, 1, 0)) AS TOTAL_ZEROS, " +
                "SUM(IF(result = 1, 1, 0)) AS TOTAL_ONES, " +
                "game_date " +
                "FROM player, games " +
                "WHERE player.user_id_long = '" + userIdLong + "' AND player.games_id = games.id GROUP BY MONTH (game_date);";

        if (statement.executeQuery(sql) == null) {
            getMyAllStatistic(userIdLong);
        }

        return statement.executeQuery(sql);
    }
}