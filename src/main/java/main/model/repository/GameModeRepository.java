package main.model.repository;

import main.model.entity.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GameModeRepository extends JpaRepository<GameMode, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM GameMode g WHERE g.userIdLong = :userIdLong")
    void deleteGameMode(@Param("userIdLong") String userIdLong);
}
