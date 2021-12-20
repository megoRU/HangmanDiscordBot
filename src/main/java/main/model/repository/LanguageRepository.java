package main.model.repository;

import main.model.entity.Language;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LanguageRepository extends CrudRepository<Language, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM Language l WHERE l.userIdLong = :userIdLong")
    void deleteLanguage(@Param("userIdLong") Long userIdLong);
}