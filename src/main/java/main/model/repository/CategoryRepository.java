package main.model.repository;

import main.model.entity.Category;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryRepository extends CrudRepository<Category, String> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM Category c WHERE c.userIdLong = :userIdLong")
    void deleteCategory(@Param("userIdLong") Long userIdLong);
}