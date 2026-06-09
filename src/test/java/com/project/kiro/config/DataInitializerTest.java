package com.project.kiro.config;

import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataInitializer}.
 *
 * Uses @SpringBootTest to load the full application context so that the
 * ApplicationRunner (DataInitializer) executes on startup, seeding the
 * default categories.
 *
 * Validates Requirement 5.10
 */
@SpringBootTest
class DataInitializerTest {

    private static final List<String> EXPECTED_DEFAULT_CATEGORIES = List.of(
            "Food",
            "Transport",
            "Utilities",
            "Entertainment",
            "Healthcare",
            "Shopping",
            "Other"
    );

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * After application startup, all 7 default categories must exist in the database.
     *
     * Validates: Requirements 5.10
     */
    @Test
    void dataInitializer_seedsAllSevenDefaultCategories_onStartup() {
        List<Category> allCategories = categoryRepository.findAll();

        Set<String> categoryNames = allCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        assertThat(categoryNames).containsAll(EXPECTED_DEFAULT_CATEGORIES);
    }

    /**
     * Each default category must have a non-null ID (persisted) and a matching
     * lowercase name stored in the nameLower field.
     *
     * Validates: Requirements 5.10
     */
    @Test
    void dataInitializer_eachDefaultCategory_hasIdAndCorrectNameLower() {
        List<Category> allCategories = categoryRepository.findAll();

        for (String expectedName : EXPECTED_DEFAULT_CATEGORIES) {
            Category match = allCategories.stream()
                    .filter(c -> c.getName().equals(expectedName))
                    .findFirst()
                    .orElse(null);

            assertThat(match)
                    .as("Expected default category '%s' to exist", expectedName)
                    .isNotNull();
            assertThat(match.getId())
                    .as("Category '%s' should have a non-null ID", expectedName)
                    .isNotNull();
            assertThat(match.getNameLower())
                    .as("Category '%s' should have nameLower = '%s'", expectedName, expectedName.toLowerCase())
                    .isEqualTo(expectedName.toLowerCase());
        }
    }

    /**
     * Running the initializer a second time (idempotency check) must not create
     * duplicate categories.
     *
     * Validates: Requirements 5.10
     */
    @Test
    void dataInitializer_isIdempotent_doesNotCreateDuplicates() throws Exception {
        // Re-run the initializer manually
        DataInitializer initializer = new DataInitializer(categoryRepository);
        initializer.run(null);

        // Count how many times each default category name appears
        List<Category> allCategories = categoryRepository.findAll();

        for (String expectedName : EXPECTED_DEFAULT_CATEGORIES) {
            long count = allCategories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(expectedName))
                    .count();

            assertThat(count)
                    .as("Category '%s' should appear exactly once", expectedName)
                    .isEqualTo(1L);
        }
    }
}
