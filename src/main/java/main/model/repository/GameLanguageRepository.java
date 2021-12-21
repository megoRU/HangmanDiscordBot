package main.model.repository;

import main.model.entity.GameLanguage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GameLanguageRepository extends CrudRepository<GameLanguage, String> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM GameLanguage gm WHERE gm.userIdLong = :userIdLong")
    void deleteGameLanguage(@Param("userIdLong") String userIdLong);
}
