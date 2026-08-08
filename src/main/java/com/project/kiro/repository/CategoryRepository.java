package com.project.kiro.repository;

import com.project.kiro.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its lowercase name (used for case-insensitive uniqueness checks).
     *
     * @param nameLower the lowercase version of the category name
     * @return an Optional containing the matching category, or empty if not found
     */
    Optional<Category> findByNameLower(String nameLower);

    /**
     * Return all categories sorted alphabetically by name (ascending).
     * Satisfies Requirement 5.11.
     *
     * @return list of all categories ordered by name ascending
     */
    List<Category> findAllByOrderByNameAsc();
}
