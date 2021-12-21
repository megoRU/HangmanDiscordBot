package main.model.repository;

import main.model.entity.Prefix;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PrefixRepository extends CrudRepository<Prefix, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM Prefix p WHERE p.serverId = :serverId")
    void deletePrefix(@Param("serverId") Long serverId);


    @Query(value = "SELECT p FROM Prefix p")
    List<Prefix> getPrefix();
}
