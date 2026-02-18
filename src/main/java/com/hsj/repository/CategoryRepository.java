package com.hsj.repository;

import com.hsj.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndDeletedFalse(Long id);

    List<Category> findAllByDeletedFalseOrderBySortOrder();

    List<Category> findByParentIsNullAndDeletedFalseOrderBySortOrder();

    boolean existsByNameAndDeletedFalse(String name);
}
