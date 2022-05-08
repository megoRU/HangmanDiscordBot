package main.model.repository;

import main.model.entity.Game;
import main.model.repository.impl.StatisticGlobal;
import main.model.repository.impl.StatisticMy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GamesRepository extends JpaRepository<Game, Long> {

    @Query(value = "SELECT COUNT(g.id) AS COUNT_GAMES, " +
            "SUM (CASE WHEN g.result = false THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
            "SUM (CASE WHEN g.result = true THEN 1 ELSE 0 END) AS TOTAL_ONES " +
            "FROM Player pl, Game g WHERE pl.userIdLong = :userIdLong " +
            "AND pl.games_id.id = g.id")
    String getStatistic(@Param("userIdLong") Long userIdLong);

    @Query(value = "SELECT COUNT(*) AS count, game_date AS gameDate FROM games GROUP BY YEAR(game_date), MONTH(game_date) ORDER BY `gameDate` DESC LIMIT 8", nativeQuery = true)
    List<StatisticGlobal> getAllStatistic();

    @Query(value = "SELECT SUM(IF(result = 0, 1, 0)) AS TOTAL_ZEROS, " +
            "SUM(IF(result = 1, 1, 0)) AS TOTAL_ONES, " +
            "game_date AS gameDate " +
            "FROM player, games " +
            "WHERE player.user_id_long = :userIdLong AND player.games_id = games.id GROUP BY MONTH (game_date) ORDER BY `gameDate` DESC LIMIT 8", nativeQuery = true)
    List<StatisticMy> getAllMyStatistic(@Param("userIdLong") String userIdLong);

    @Modifying
    @Transactional
    @Query(value = "DELETE g FROM games g " +
            "JOIN player p on g.id = p.games_id " +
            "WHERE p.user_id_long = :userIdLong", nativeQuery = true)
    void deleteAllMyData(@Param("userIdLong") Long userIdLong);
}
