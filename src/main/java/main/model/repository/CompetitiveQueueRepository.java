package main.model.repository;

import main.model.entity.CompetitiveQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitiveQueueRepository extends JpaRepository<CompetitiveQueue, Long> {

}