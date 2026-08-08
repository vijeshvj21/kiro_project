package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.model.Category;
import com.project.kiro.model.Expense;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for CategoryService using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class CategoryServicePropertyTest {

    // -------------------------------------------------------------------------
    // Spring context — started once for the entire test class
    // -------------------------------------------------------------------------

    private static final ConfigurableApplicationContext ctx;
    private static final MockMvc mockMvc;
    private static final ObjectMapper objectMapper;
    private static final CategoryRepository categoryRepository;
    private static final ExpenseRepository expenseRepository;

    static {
        ctx = SpringApplication.run(KiroApplication.class,
                "--spring.datasource.url=jdbc:h2:mem:propertytestdb;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.h2.console.enabled=false",
                "--server.port=0");

        // ConfigurableApplicationContext from SpringApplication.run() is also a
        // WebApplicationContext when running a web application — cast directly.
        mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
        objectMapper = ctx.getBean(ObjectMapper.class);
        categoryRepository = ctx.getBean(CategoryRepository.class);
        expenseRepository = ctx.getBean(ExpenseRepository.class);
    }

    // -------------------------------------------------------------------------
    // Helper: POST /api/v1/categories
    // -------------------------------------------------------------------------

    private MvcResult createCategoryRaw(String name) throws Exception {
        CategoryRequest req = new CategoryRequest(name);
        return mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    private CategoryResponse createCategoryOk(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helper: DELETE /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    private int deleteCategoryStatus(Long id) throws Exception {
        return mockMvc.perform(delete("/api/v1/categories/" + id))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    // -------------------------------------------------------------------------
    // Helper: GET /api/v1/categories
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<CategoryResponse> getAllCategories() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryResponse.class));
    }

    // -------------------------------------------------------------------------
    // Helper: create an expense linked to a category
    // -------------------------------------------------------------------------

    private Expense createExpenseForCategory(Category category) {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("10.00"))
                .expenseDate(LocalDate.now())
                .category(category)
                .description("test expense")
                .build();
        return expenseRepository.save(expense);
    }

    // -------------------------------------------------------------------------
    // Helper: clean up categories created during a test (by name)
    // -------------------------------------------------------------------------

    private void cleanupCategoryByName(String name) {
        categoryRepository.findByNameLower(name.toLowerCase())
                .ifPresent(c -> {
                    expenseRepository.findAll().stream()
                            .filter(e -> e.getCategory().getId().equals(c.getId()))
                            .forEach(expenseRepository::delete);
                    categoryRepository.delete(c);
                });
    }

    // =========================================================================
    // Property 17: Valid category names are always persisted and returned with a unique ID
    // Validates: Requirements 5.1
    // =========================================================================

    // Feature: expense-tracker, Property 17: Valid category names are always persisted and returned with a unique ID
    @Property(tries = 50)
    void property17_validCategoryNameIsPersistedWithUniqueId(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String baseName) throws Exception {
        // Append UUID to guarantee uniqueness across tries and against seeded data
        String uniqueName = baseName + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            CategoryResponse response = createCategoryOk(uniqueName);

            assertThat(response.getId())
                    .as("Created category must have a non-null ID")
                    .isNotNull();
            assertThat(response.getId())
                    .as("Created category ID must be positive")
                    .isGreaterThan(0L);
            assertThat(response.getName())
                    .as("Returned name must match the trimmed input")
                    .isEqualTo(uniqueName.trim());
        } finally {
            cleanupCategoryByName(uniqueName);
        }
    }

    // =========================================================================
    // Property 18: Duplicate category names (case-insensitive) are always rejected with 409
    // Validates: Requirements 5.2, 5.6
    // =========================================================================

    // Feature: expense-tracker, Property 18: Duplicate category names (case-insensitive) are always rejected with 409
    @Property(tries = 50)
    void property18_duplicateCategoryNameIsRejectedWith409(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String baseName) throws Exception {
        String uniqueName = baseName + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            // Create the original category
            createCategoryOk(uniqueName);

            // Attempt to create a duplicate with the same name (exact case)
            MvcResult duplicateResult = createCategoryRaw(uniqueName);
            assertThat(duplicateResult.getResponse().getStatus())
                    .as("Exact duplicate name must return 409 Conflict")
                    .isEqualTo(409);

            // Attempt to create a duplicate with uppercase name
            MvcResult upperResult = createCategoryRaw(uniqueName.toUpperCase());
            assertThat(upperResult.getResponse().getStatus())
                    .as("Uppercase duplicate name must return 409 Conflict")
                    .isEqualTo(409);

            // Attempt to create a duplicate with lowercase name
            MvcResult lowerResult = createCategoryRaw(uniqueName.toLowerCase());
            assertThat(lowerResult.getResponse().getStatus())
                    .as("Lowercase duplicate name must return 409 Conflict")
                    .isEqualTo(409);
        } finally {
            cleanupCategoryByName(uniqueName);
        }
    }

    // =========================================================================
    // Property 19: Whitespace-only category names are always rejected with 400
    // Validates: Requirements 5.3
    // =========================================================================

    // Feature: expense-tracker, Property 19: Whitespace-only category names are always rejected with 400
    @Property(tries = 50)
    void property19_whitespaceOnlyCategoryNameIsRejectedWith400(
            @ForAll("whitespaceStrings") String whitespaceOnly) throws Exception {

        MvcResult result = createCategoryRaw(whitespaceOnly);
        assertThat(result.getResponse().getStatus())
                .as("Whitespace-only name must return 400 Bad Request")
                .isEqualTo(400);
    }

    @Provide
    Arbitrary<String> whitespaceStrings() {
        // Generate strings of 1–20 whitespace characters (spaces and tabs)
        Arbitrary<Character> whitespaceChars = Arbitraries.of(' ', '\t');
        return whitespaceChars.list().ofMinSize(1).ofMaxSize(20)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    chars.forEach(sb::append);
                    return sb.toString();
                });
    }

    // =========================================================================
    // Property 20: Category names exceeding 100 characters are always rejected with 400
    // Validates: Requirements 5.4
    // =========================================================================

    // Feature: expense-tracker, Property 20: Category names exceeding 100 characters are always rejected with 400
    @Property(tries = 50)
    void property20_categoryNameExceeding100CharsIsRejectedWith400(
            @ForAll @AlphaChars @StringLength(min = 101, max = 200) String longName) throws Exception {

        MvcResult result = createCategoryRaw(longName);
        assertThat(result.getResponse().getStatus())
                .as("Name of length %d must return 400 Bad Request", longName.length())
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 21: Deleting a category with no expenses succeeds (round-trip)
    // Validates: Requirements 5.7
    // =========================================================================

    // Feature: expense-tracker, Property 21: Deleting a category with no expenses succeeds (round-trip)
    @Property(tries = 30)
    void property21_deletingCategoryWithNoExpensesSucceeds(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String baseName) throws Exception {
        String uniqueName = baseName + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Create the category
        CategoryResponse created = createCategoryOk(uniqueName);
        Long categoryId = created.getId();

        // Delete it — should return 204
        int deleteStatus = deleteCategoryStatus(categoryId);
        assertThat(deleteStatus)
                .as("Deleting a category with no expenses must return 204 No Content")
                .isEqualTo(204);

        // Verify it no longer exists — GET all categories should not contain it
        List<CategoryResponse> allCategories = getAllCategories();
        boolean stillExists = allCategories.stream()
                .anyMatch(c -> c.getId().equals(categoryId));
        assertThat(stillExists)
                .as("Deleted category (id=%d) must not appear in the category list", categoryId)
                .isFalse();
    }

    // =========================================================================
    // Property 22: Deleting a category with associated expenses is always rejected with 409
    // Validates: Requirements 5.8
    // =========================================================================

    // Feature: expense-tracker, Property 22: Deleting a category with associated expenses is always rejected with 409
    @Property(tries = 30)
    void property22_deletingCategoryWithExpensesIsRejectedWith409(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String baseName) throws Exception {
        String uniqueName = baseName + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Create the category
        CategoryResponse created = createCategoryOk(uniqueName);
        Long categoryId = created.getId();

        // Fetch the managed Category entity and create an expense linked to it
        Category categoryEntity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AssertionError("Category not found after creation: " + categoryId));
        Expense expense = createExpenseForCategory(categoryEntity);

        try {
            // Attempt to delete — should return 409 Conflict
            int deleteStatus = deleteCategoryStatus(categoryId);
            assertThat(deleteStatus)
                    .as("Deleting a category with associated expenses must return 409 Conflict")
                    .isEqualTo(409);
        } finally {
            // Cleanup: remove expense first, then category
            expenseRepository.delete(expense);
            categoryRepository.deleteById(categoryId);
        }
    }

    // =========================================================================
    // Property 23: Category list is always sorted alphabetically by name
    // Validates: Requirements 5.11
    // =========================================================================

    // Feature: expense-tracker, Property 23: Category list is always sorted alphabetically by name
    @Property(tries = 30)
    void property23_categoryListIsAlwaysSortedAlphabetically(
            @ForAll("uniqueCategoryNameLists") List<String> names) throws Exception {

        List<Long> createdIds = new ArrayList<>();

        try {
            // Create all categories
            for (String name : names) {
                CategoryResponse created = createCategoryOk(name);
                createdIds.add(created.getId());
            }

            // Fetch the full list
            List<CategoryResponse> allCategories = getAllCategories();

            // Verify the list is sorted ascending by name (case-sensitive, matching DB ORDER BY name ASC)
            for (int i = 0; i < allCategories.size() - 1; i++) {
                String current = allCategories.get(i).getName();
                String next = allCategories.get(i + 1).getName();
                assertThat(current.compareTo(next))
                        .as("Category '%s' at index %d should come before '%s' at index %d",
                                current, i, next, i + 1)
                        .isLessThanOrEqualTo(0);
            }
        } finally {
            // Cleanup created categories
            for (Long id : createdIds) {
                try {
                    categoryRepository.deleteById(id);
                } catch (Exception ignored) {
                    // Best-effort cleanup
                }
            }
        }
    }

    @Provide
    Arbitrary<List<String>> uniqueCategoryNameLists() {
        // Generate 2–5 unique alpha names with UUID suffix to avoid collisions
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .list().ofMinSize(2).ofMaxSize(5)
                .map(baseNames -> {
                    List<String> unique = new ArrayList<>();
                    for (String base : baseNames) {
                        unique.add(base + "-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                    return unique;
                });
    }
}
