package com.project.kiro.repository;

import com.project.kiro.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for {@link CategoryRepository}.
 *
 * Uses @SpringBootTest with @Transactional so each test runs in a transaction
 * that is rolled back after the test, keeping the database clean.
 *
 * Note: DataInitializer seeds 7 default categories on startup, so tests
 * that check counts must account for those pre-existing categories.
 *
 * Validates Requirements 5.10, 5.11
 */
@SpringBootTest
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    // -------------------------------------------------------------------------
    // findByNameLower tests
    // -------------------------------------------------------------------------

    @Test
    void findByNameLower_returnsCategory_whenExists() {
        // "food" is seeded by DataInitializer
        Optional<Category> result = categoryRepository.findByNameLower("food");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Food");
    }

    @Test
    void findByNameLower_returnsEmpty_whenNotExists() {
        Optional<Category> result = categoryRepository.findByNameLower("nonexistent_category_xyz");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findAllByOrderByNameAsc tests — Requirement 5.11
    // -------------------------------------------------------------------------

    @Test
    void findAllByOrderByNameAsc_returnsCategoriesSortedAlphabetically() {
        List<Category> result = categoryRepository.findAllByOrderByNameAsc();

        // Must have at least the 7 default categories
        assertThat(result).hasSizeGreaterThanOrEqualTo(7);

        // Verify the list is sorted ascending by name
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getName().compareToIgnoreCase(result.get(i + 1).getName()))
                    .as("Category at index %d ('%s') should come before index %d ('%s')",
                            i, result.get(i).getName(), i + 1, result.get(i + 1).getName())
                    .isLessThanOrEqualTo(0);
        }
    }

    @Test
    void findAllByOrderByNameAsc_defaultCategoriesArePresent() {
        List<Category> result = categoryRepository.findAllByOrderByNameAsc();

        List<String> names = result.stream().map(Category::getName).toList();
        assertThat(names).contains("Entertainment", "Food", "Healthcare",
                "Other", "Shopping", "Transport", "Utilities");
    }

    // -------------------------------------------------------------------------
    // Persistence tests
    // -------------------------------------------------------------------------

    @Test
    void save_persistsCategoryWithGeneratedId() {
        Category saved = categoryRepository.save(Category.builder()
                .name("TestCategory")
                .nameLower("testcategory")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("TestCategory");
        assertThat(saved.getNameLower()).isEqualTo("testcategory");
    }

    @Test
    void delete_removesCategoryFromDatabase() {
        Category saved = categoryRepository.save(Category.builder()
                .name("TempCategory")
                .nameLower("tempcategory")
                .build());

        Long savedId = saved.getId();
        categoryRepository.delete(saved);

        assertThat(categoryRepository.findById(savedId)).isEmpty();
    }
}
