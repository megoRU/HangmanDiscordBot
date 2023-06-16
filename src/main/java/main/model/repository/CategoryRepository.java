package main.model.repository;

import main.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryRepository extends JpaRepository<Category, String> {

    @Transactional
    @Modifying
    void deleteByUserIdLong(Long userIdLong);
}