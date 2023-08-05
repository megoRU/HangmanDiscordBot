package main.model.repository;

import jakarta.transaction.Transactional;
import main.model.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    UserSettings getByUserIdLong(Long userIdLong);

    @Transactional
    @Modifying
    void deleteByUserIdLong(Long userIdLong);
}