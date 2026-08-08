package com.project.kiro.config;

import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the 7 default categories on application startup if they do not already exist.
 * The check is case-insensitive via the {@code nameLower} field, making this initializer idempotent.
 *
 * Satisfies Requirement 5.10.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Food",
            "Transport",
            "Utilities",
            "Entertainment",
            "Healthcare",
            "Shopping",
            "Other"
    );

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String name : DEFAULT_CATEGORIES) {
            String nameLower = name.toLowerCase();
            if (categoryRepository.findByNameLower(nameLower).isEmpty()) {
                Category category = Category.builder()
                        .name(name)
                        .nameLower(nameLower)
                        .build();
                categoryRepository.save(category);
            }
        }
    }
}
