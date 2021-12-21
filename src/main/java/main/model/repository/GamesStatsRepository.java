package main.model.repository;

import main.model.entity.Game;
import main.model.entity.Language;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GamesStatsRepository extends CrudRepository<Game, Long> {


//    @Query(value =  "SELECT COUNT(Game.id) AS COUNT_GAMES, " +
//                    "SUM(CASE WHEN Game.result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
//                    "SUM(CASE WHEN Game.result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES " +
//                    "FROM Player, Game WHERE Player.userIdLong = :userIdLong " +
//                    "AND Player.games_id = Game.id ")
//    void getStatistic(@Param("userIdLong") Long userIdLong);


//     public String getStatistic(String userIdLong) throws NullPointerException {
//        try {
//            Statement statement = DataBase.getConnection().createStatement();
//            String sql = "SELECT COUNT(games_id) AS COUNT_GAMES, " +
//                    "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
//                    "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES " +
//                    "FROM player, games WHERE player.user_id_long = '" + userIdLong + "' " +
//                    "AND player.games_id = games.id";
//            ResultSet rs = statement.executeQuery(sql);
//
//            while (rs.next()) {
//                return rs.getString("COUNT_GAMES") + " " +
//                        rs.getString("TOTAL_ZEROS") + " " +
//                        rs.getString("TOTAL_ONES");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        throw new NullPointerException();
//    }
}
