package main.model.repository;

import main.model.entity.ActiveHangman;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface HangmanGameRepository extends CrudRepository<ActiveHangman, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE ActiveHangman ah SET " +
            "ah.currentHiddenWord = :currentHiddenWord, " +
            "ah.guesses = :guesses, " +
            "ah.hangmanErrors = :hangmanErrors " +
            "WHERE ah.userIdLong = :userId")
    void updateGame(@Param("userId") Long userId,
                    @Param("currentHiddenWord") String currentHiddenWord,
                    @Param("guesses") String guesses,
                    @Param("hangmanErrors") Integer hangmanErrors);

    @Query(value = "SELECT MAX(id) AS id FROM Game")
    Integer getCountGames();


    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ActiveHangman ah WHERE ah.userIdLong = :userIdLong")
    void deleteActiveGame(@Param("userIdLong") Long userIdLong);

    @Query(value = "SELECT ah FROM ActiveHangman ah")
    List<ActiveHangman> getAllActiveGames();

    //
}
