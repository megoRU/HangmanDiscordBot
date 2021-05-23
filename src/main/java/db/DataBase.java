package db;

import config.Config;

import java.sql.*;

public class DataBase {

//  INSERT INTO games(id, result, game_date) VALUES (1, true, "2021-05-23 14:25:10");
//  SELECT * FROM player, games WHERE player.games_id = games.id;

//  сделано:
//  CREATE TABLE
//
//  games(
//          id int NOT NULL AUTO_INCREMENT,
//          result BOOLEAN,
//          game_date DATETIME,
//          PRIMARY KEY (id)
//);

//CREATE TABLE
//
//  player(
//    `user_id_long` bigint(30) NOT NULL,
//
//  games_id int,
//
//  FOREIGN KEY(games_id)
//
//  REFERENCES games(id))
//);


//  Сделано:
//  CREATE TABLE `ActiveHangman`(`user_id_long` bigint(30) NOT NULL,
//   `message_id_long` bigint(30) NOT NULL,
//   `channel_id_long` bigint(30) NOT NULL,
//   `guild_long_id` bigint(30) NOT NULL,
//   `wordList` varchar(255),
//   `index` varchar(255),
//   `usedLetters` varchar(255),
//   `count` int(6) NOT NULL,
//   `count2` int(6) NOT NULL,
//  PRIMARY KEY (`user_id_long`),
//  UNIQUE KEY `user_id_long` (`user_id_long`))
//  ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

//   CREATE TABLE `language` (
//   user_id_long` varchar(255) NOT NULL,
//   `language` varchar(255) NOT NULL,
//   PRIMARY KEY (`user_id_long`),
//   UNIQUE KEY `user_id_long` (`user_id_long`))
//   ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  private static volatile Connection connection;
  private static volatile DataBase dataBase;

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

  private DataBase() {}

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
  public void addResultGame(long id, boolean result, long game_date) {
    try {
      String sql = "INSERT INTO games ("
          + "id, "
          + "result, "
          + "game_date) "
          + "VALUES (?, ?, ?)";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.setLong(1, id);
      preparedStatement.setBoolean(2, result);
      preparedStatement.setTimestamp(3, new Timestamp(game_date));
      preparedStatement.execute();
      preparedStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Добавляем связь id игры с игроком
  public void addResultPlayer(long userIdLong, int games_id) {
    try {
      String sql = "INSERT INTO player ("
              + "user_id_long, "
              + "games_id) "
              + "VALUES (?, ?)";
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
      String sql = "INSERT INTO language (user_id_long, language) VALUES (?, ?)";
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
      String sql = "INSERT INTO game_language (user_id_long, language) VALUES (?, ?)";
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


  /**
   * Подсчитываем колличество записей в таблице
   **/
  public synchronized int getCountGames() {
    try {
      Statement statement = getConnection().createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT COUNT(id) AS id FROM games");
      if (resultSet.next()) {
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