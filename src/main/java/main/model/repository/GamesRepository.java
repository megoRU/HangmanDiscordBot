package main.model.repository;

import main.model.entity.Game;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GamesRepository extends CrudRepository<Game, Long> {

    @Query(value =  "SELECT COUNT(g.id) AS COUNT_GAMES, " +
            "SUM (CASE WHEN g.result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS, " +
            "SUM (CASE WHEN g.result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES " +
            "FROM Player pl, Game g WHERE pl.userIdLong = :userIdLong " +
            "AND pl.games_id.id = g.id")
    String getStatistic(@Param("userIdLong") Long userIdLong);

}
