package main.model.repository;

import main.model.entity.Game;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HangmanGameRepository extends CrudRepository<Game, Long> {

    @Query(value = "UPDATE ActiveHangman ah SET " +
            "ah.currentHiddenWord = :currentHiddenWord, " +
            "ah.guesses = :guesses, " +
            "ah.hangmanErrors = :hangmanErrors " +
            "WHERE ah.userIdLong = :userId")
    void updateGame(@Param("userId") String userId,
                    @Param("currentHiddenWord") String currentHiddenWord,
                    @Param("guesses") String guesses,
                    @Param("hangmanErrors") Integer hangmanErrors);

//    @Query(value = "UPDATE ActiveHangman ah SET " +
//            "ah.currentHiddenWord = :currentHiddenWord, " +
//            "ah.guesses = :guesses, " +
//            "ah.hangmanErrors = :hangmanErrors " +
//            "WHERE ah.userIdLong = :userId")
//    void createGame();

    @Query(value = "SELECT MAX(id) AS id FROM Game")
    Integer getCountGames();





    //
}
