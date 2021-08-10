package db;

import config.Config;
import hangman.HangmanRegistry;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DataBase {

    private static volatile Connection connection;
    private static volatile DataBase dataBase;

    private DataBase() {
    }

    //Создаем один коннект на программу
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (DataBase.class) {
                if (connection == null || connection.isClosed()) {
                    connection = DriverManager.getConnection(
                            Config.getHangmanConnection(),
                            Config.getHangmanUser(),
                            Config.getHangmanPass());
                }
            }
        }
        return connection;
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

    public void deleteActiveGame(String userIdLong) {
        try {
            String sql = "DELETE FROM ActiveHangman WHERE user_id_long = '" + userIdLong + "'";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateGame(String userId, String messageIdLong, String channelId, String guildId,
                           String WORD, String currentHiddenWord,
                           String guesses, String hangmanErrors) {
        try {
            String sql = "REPLACE INTO ActiveHangman " +
                    "(user_id_long, message_id_long, channel_id_long, " +
                    "guild_long_id, word, current_hidden_word, " +
                    "guesses, hangman_errors) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, messageIdLong);
            preparedStatement.setString(3, channelId);
            preparedStatement.setString(4, guildId);
            preparedStatement.setString(5, WORD);
            preparedStatement.setString(6, currentHiddenWord);
            preparedStatement.setString(7, guesses);
            preparedStatement.setString(8, hangmanErrors);
            preparedStatement.execute();

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
    public void removePrefixFromDB(String serverId) {
        try {
            String sql = "DELETE FROM prefixs WHERE serverId='" + serverId + "'";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.execute(sql);
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public void removeLanguageFromDB(String userIdLong) {
        try {
            String sql = "DELETE FROM language WHERE user_id_long='" + userIdLong + "'";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.execute(sql);
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public void removeGameLanguageFromDB(String userIdLong) {
        try {
            String sql = "DELETE FROM game_language WHERE user_id_long='" + userIdLong + "'";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.execute(sql);
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getStatistic(String userIdLong) {
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
        return "";
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
}