package main.model.repository;

import main.model.entity.Game;
import main.model.repository.impl.PlayerWins;
import main.model.repository.impl.StatisticGlobal;
import main.model.repository.impl.StatisticMy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface GamesRepository extends JpaRepository<Game, Long> {

    @Query(value = "SELECT COUNT(g.id) AS COUNT_GAMES, " +
                    "SUM (CASE WHEN g.result = false THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
                    "SUM (CASE WHEN g.result = true THEN 1 ELSE 0 END) AS TOTAL_ONES " +
                    "FROM Game g WHERE g.userIdLong = :userIdLong")
    String getStatistic(@Param("userIdLong") Long userIdLong);

    @Query(value = "SELECT COUNT(*) AS count, game_date AS gameDate FROM games GROUP BY YEAR(game_date), MONTH(game_date) ORDER BY `gameDate` DESC LIMIT 8", nativeQuery = true)
    List<StatisticGlobal> getAllStatistic();

    @Query(value = "SELECT SUM(IF(result = 0, 1, 0)) AS TOTAL_ZEROS, " +
            "SUM(IF(result = 1, 1, 0)) AS TOTAL_ONES, " +
            "game_date AS gameDate " +
            "FROM games g " +
            "WHERE g.user_id_long = :userIdLong GROUP BY YEAR(game_date), MONTH(game_date) ORDER BY `gameDate` DESC LIMIT 8", nativeQuery = true)
    List<StatisticMy> getAllMyStatistic(@Param("userIdLong") String userIdLong);

    @Modifying
    @Transactional
    void deleteGameByUserIdLong(Long userIdLong);

    @Query(value = "SELECT user_id_long as id, COUNT(*) as wins FROM games " +
            "WHERE result = true AND MONTH(game_date) = MONTH(NOW()) " +
            "AND YEAR(game_date) = YEAR(NOW()) GROUP BY user_id_long " +
            "ORDER BY `wins` DESC LIMIT 10", nativeQuery = true)
    List<PlayerWins> findGamesForCurrentMonth();
}
