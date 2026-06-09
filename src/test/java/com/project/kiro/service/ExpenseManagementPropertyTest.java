package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import net.jqwik.api.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Property-based tests for expense management (update, delete, list ordering) using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class ExpenseManagementPropertyTest {

    // -------------------------------------------------------------------------
    // Spring context — started once for the entire test class
    // -------------------------------------------------------------------------

    private static final ConfigurableApplicationContext ctx;
    private static final MockMvc mockMvc;
    private static final ObjectMapper objectMapper;
    private static final CategoryRepository categoryRepository;
    private static final ExpenseRepository expenseRepository;
    private static final Long foodCategoryId;

    static {
        ctx = SpringApplication.run(KiroApplication.class,
                "--spring.datasource.url=jdbc:h2:mem:expensemgmtproptest;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.h2.console.enabled=false",
                "--server.port=0");

        mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
        objectMapper = ctx.getBean(ObjectMapper.class);
        categoryRepository = ctx.getBean(CategoryRepository.class);
        expenseRepository = ctx.getBean(ExpenseRepository.class);

        // Fetch the "Food" category ID seeded by DataInitializer
        Category food = categoryRepository.findByNameLower("food")
                .orElseThrow(() -> new IllegalStateException("'Food' category was not seeded"));
        foodCategoryId = food.getId();
    }

    // -------------------------------------------------------------------------
    // Helper: POST /api/v1/expenses — create and return created expense
    // -------------------------------------------------------------------------

    private ExpenseResponse createExpense(BigDecimal amount, LocalDate date, String description) throws Exception {
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(date)
                .categoryId(foodCategoryId)
                .description(description)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Expense creation must return 201 Created")
                .isEqualTo(201);

        return objectMapper.readValue(result.getResponse().getContentAsString(), ExpenseResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helper: PUT /api/v1/expenses/{id}
    // -------------------------------------------------------------------------

    private MvcResult updateExpenseRaw(Long id, BigDecimal amount, LocalDate date, String description) throws Exception {
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(date)
                .categoryId(foodCategoryId)
                .description(description)
                .build();

        return mockMvc.perform(put("/api/v1/expenses/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    // -------------------------------------------------------------------------
    // Helper: DELETE /api/v1/expenses/{id}
    // -------------------------------------------------------------------------

    private int deleteExpense(Long id) throws Exception {
        return mockMvc.perform(delete("/api/v1/expenses/" + id))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    // -------------------------------------------------------------------------
    // Helper: GET /api/v1/expenses/{id}
    // -------------------------------------------------------------------------

    private int getExpenseStatus(Long id) throws Exception {
        return mockMvc.perform(get("/api/v1/expenses/" + id))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    // -------------------------------------------------------------------------
    // Helper: GET /api/v1/expenses
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<ExpenseResponse> getAllExpenses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/expenses"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("GET /api/v1/expenses must return 200 OK")
                .isEqualTo(200);

        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ExpenseResponse.class));
    }

    // =========================================================================
    // Property 7: Valid expense updates are persisted and returned correctly
    // Validates: Requirements 2.1
    // =========================================================================

    // Feature: expense-tracker, Property 7: Valid expense updates are persisted and returned correctly
    @Property(tries = 20)
    void property7_validExpenseUpdateIsPersistedAndReturnedCorrectly(
            @ForAll("validAmounts") BigDecimal originalAmount,
            @ForAll("validAmounts") BigDecimal updatedAmount,
            @ForAll("pastOrPresentDates") LocalDate originalDate,
            @ForAll("pastOrPresentDates") LocalDate updatedDate) throws Exception {

        ExpenseResponse created = createExpense(originalAmount, originalDate, "original description");
        Long id = created.getId();

        try {
            MvcResult updateResult = updateExpenseRaw(id, updatedAmount, updatedDate, "updated description");

            assertThat(updateResult.getResponse().getStatus())
                    .as("Valid update must return 200 OK")
                    .isEqualTo(200);

            ExpenseResponse updated = objectMapper.readValue(
                    updateResult.getResponse().getContentAsString(), ExpenseResponse.class);

            assertThat(updated.getId())
                    .as("Updated expense ID must remain the same as the original")
                    .isEqualTo(id);
            assertThat(updated.getAmount())
                    .as("Updated amount must match the requested amount")
                    .isEqualByComparingTo(updatedAmount);
            assertThat(updated.getExpenseDate())
                    .as("Updated date must match the requested date")
                    .isEqualTo(updatedDate);
            assertThat(updated.getDescription())
                    .as("Updated description must match the requested description")
                    .isEqualTo("updated description");
            assertThat(updated.getCategoryId())
                    .as("Category ID must remain unchanged after update")
                    .isEqualTo(foodCategoryId);
        } finally {
            deleteExpense(id);
        }
    }

    // =========================================================================
    // Property 8: Updates and deletes on non-existent IDs always return 404
    // Validates: Requirements 2.3, 2.6
    // =========================================================================

    // Feature: expense-tracker, Property 8: Updates and deletes on non-existent IDs always return 404
    @Property(tries = 20)
    void property8_updatesAndDeletesOnNonExistentIdsReturn404(
            @ForAll("nonExistentIds") Long nonExistentId) throws Exception {

        // PUT on a non-existent ID must return 404
        MvcResult putResult = updateExpenseRaw(nonExistentId, new BigDecimal("10.00"), LocalDate.now(), "test");
        assertThat(putResult.getResponse().getStatus())
                .as("PUT on non-existent ID %d must return 404", nonExistentId)
                .isEqualTo(404);

        // DELETE on a non-existent ID must return 404
        int deleteStatus = deleteExpense(nonExistentId);
        assertThat(deleteStatus)
                .as("DELETE on non-existent ID %d must return 404", nonExistentId)
                .isEqualTo(404);
    }

    // =========================================================================
    // Property 9: Delete then lookup returns 404 (round-trip deletion)
    // Validates: Requirements 2.4
    // =========================================================================

    // Feature: expense-tracker, Property 9: Delete then lookup returns 404 (round-trip deletion)
    @Property(tries = 20)
    void property9_deleteThenLookupReturns404(
            @ForAll("validAmounts") BigDecimal amount,
            @ForAll("pastOrPresentDates") LocalDate date) throws Exception {

        // Create the expense
        ExpenseResponse created = createExpense(amount, date, "to be deleted");
        Long id = created.getId();

        // Delete must return 204 No Content
        int deleteStatus = deleteExpense(id);
        assertThat(deleteStatus)
                .as("DELETE of existing expense ID %d must return 204 No Content", id)
                .isEqualTo(204);

        // Subsequent GET must return 404
        int getStatus = getExpenseStatus(id);
        assertThat(getStatus)
                .as("GET after DELETE for expense ID %d must return 404", id)
                .isEqualTo(404);
    }

    // =========================================================================
    // Property 10: Expense list is always sorted by date descending
    // Validates: Requirements 2.7
    // =========================================================================

    // Feature: expense-tracker, Property 10: Expense list is always sorted by date descending
    @Property(tries = 20)
    void property10_expenseListIsAlwaysSortedByDateDescending(
            @ForAll("distinctPastDates") List<LocalDate> dates) throws Exception {

        List<Long> createdIds = new ArrayList<>();

        try {
            // Create one expense per date
            for (LocalDate date : dates) {
                ExpenseResponse created = createExpense(new BigDecimal("10.00"), date, "sort test");
                createdIds.add(created.getId());
            }

            // Fetch the full expense list
            List<ExpenseResponse> allExpenses = getAllExpenses();

            // Assert that the entire returned list is sorted by date descending (non-increasing)
            for (int i = 0; i < allExpenses.size() - 1; i++) {
                LocalDate current = allExpenses.get(i).getExpenseDate();
                LocalDate next = allExpenses.get(i + 1).getExpenseDate();
                assertThat(current)
                        .as("Expense at index %d (date=%s) must be >= expense at index %d (date=%s)",
                                i, current, i + 1, next)
                        .isAfterOrEqualTo(next);
            }
        } finally {
            for (Long id : createdIds) {
                try {
                    deleteExpense(id);
                } catch (Exception ignored) {
                    // Best-effort cleanup
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Arbitraries (generators)
    // -------------------------------------------------------------------------

    /**
     * Valid amounts: BigDecimal in [0.01, 999999999.99] with at most 2 decimal places.
     */
    @Provide
    Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.longs()
                .between(1L, 99_999_999_999L)
                .map(cents -> BigDecimal.valueOf(cents, 2));
    }

    /**
     * Past or present dates (today back to ~3 years ago).
     */
    @Provide
    Arbitrary<LocalDate> pastOrPresentDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.longs()
                .between(0L, 1095L)
                .map(daysBack -> today.minusDays(daysBack));
    }

    /**
     * Non-existent IDs: high values that almost certainly don't exist in the DB.
     * Uses Long.MAX_VALUE minus a small random offset to avoid collisions with real rows.
     */
    @Provide
    Arbitrary<Long> nonExistentIds() {
        return Arbitraries.longs()
                .between(Long.MAX_VALUE - 10_000L, Long.MAX_VALUE - 1L);
    }

    /**
     * A list of 3–5 distinct past dates spread across different months.
     * Uses months -1 to -12 from today, picking a fixed day in each to ensure distinctness.
     */
    @Provide
    Arbitrary<List<LocalDate>> distinctPastDates() {
        LocalDate today = LocalDate.now();
        // Generate a list of unique month offsets in [1..12] and map to the 1st of each preceding month
        return Arbitraries.integers().between(1, 12)
                .list().ofMinSize(3).ofMaxSize(5)
                .uniqueElements()
                .map(offsets -> {
                    List<LocalDate> dates = new ArrayList<>();
                    for (int offset : offsets) {
                        // Use first day of each preceding month to guarantee distinct dates
                        dates.add(today.minusMonths(offset).withDayOfMonth(1));
                    }
                    return dates;
                });
    }
}
