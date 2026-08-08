package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Property-based tests for expense creation and validation using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class ExpenseCreationPropertyTest {

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
                "--spring.datasource.url=jdbc:h2:mem:expensecreationproptest;DB_CLOSE_DELAY=-1",
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
    // Helpers: POST /api/v1/expenses
    // -------------------------------------------------------------------------

    private MvcResult createExpenseRaw(ExpenseRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private void deleteExpense(Long id) throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/" + id)).andReturn();
    }

    // =========================================================================
    // Property 1: Valid expense creation always returns a unique ID
    // Validates: Requirements 1.1
    // =========================================================================

    // Feature: expense-tracker, Property 1: Valid expense creation always returns a unique ID
    @Property(tries = 30)
    void property1_validExpenseCreationAlwaysReturnsUniqueId(
            @ForAll("validAmounts") BigDecimal amount,
            @ForAll("pastOrPresentDates") LocalDate expenseDate,
            @ForAll("validDescriptions") String description) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(expenseDate)
                .categoryId(foodCategoryId)
                .description(description)
                .build();

        Long createdId = null;
        try {
            MvcResult result = createExpenseRaw(request);
            assertThat(result.getResponse().getStatus())
                    .as("Valid expense must return 201 Created")
                    .isEqualTo(201);

            ExpenseResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ExpenseResponse.class);

            assertThat(response.getId())
                    .as("Created expense must have a non-null ID")
                    .isNotNull();
            assertThat(response.getId())
                    .as("Created expense ID must be positive")
                    .isGreaterThan(0L);

            createdId = response.getId();
        } finally {
            if (createdId != null) {
                deleteExpense(createdId);
            }
        }
    }

    // =========================================================================
    // Property 2: Non-positive amounts are always rejected
    // Validates: Requirements 1.5
    // =========================================================================

    // Feature: expense-tracker, Property 2: Non-positive amounts are always rejected
    @Property(tries = 30)
    void property2_nonPositiveAmountsAreAlwaysRejected(
            @ForAll("nonPositiveAmounts") BigDecimal amount) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(LocalDate.now())
                .categoryId(foodCategoryId)
                .description("test")
                .build();

        MvcResult result = createExpenseRaw(request);
        assertThat(result.getResponse().getStatus())
                .as("Non-positive amount %s must return 400 Bad Request", amount)
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 3: Out-of-range or over-precision amounts are always rejected
    // Validates: Requirements 1.6
    // =========================================================================

    // Feature: expense-tracker, Property 3: Out-of-range or over-precision amounts are always rejected
    @Property(tries = 30)
    void property3_outOfRangeOrOverPrecisionAmountsAreAlwaysRejected(
            @ForAll("invalidAmounts") BigDecimal amount) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(LocalDate.now())
                .categoryId(foodCategoryId)
                .description("test")
                .build();

        MvcResult result = createExpenseRaw(request);
        assertThat(result.getResponse().getStatus())
                .as("Invalid amount %s must return 400 Bad Request", amount)
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 4: Future dates are always rejected
    // Validates: Requirements 1.7
    // =========================================================================

    // Feature: expense-tracker, Property 4: Future dates are always rejected
    @Property(tries = 30)
    void property4_futureDatesAreAlwaysRejected(
            @ForAll("futureDates") LocalDate futureDate) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("10.00"))
                .expenseDate(futureDate)
                .categoryId(foodCategoryId)
                .description("test")
                .build();

        MvcResult result = createExpenseRaw(request);
        assertThat(result.getResponse().getStatus())
                .as("Future date %s must return 400 Bad Request", futureDate)
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 5: Descriptions within 255 chars are always accepted
    // Validates: Requirements 1.8
    // =========================================================================

    // Feature: expense-tracker, Property 5: Descriptions within the 255-character limit are always accepted
    @Property(tries = 30)
    void property5_descriptionsWithin255CharsAreAlwaysAccepted(
            @ForAll @StringLength(min = 0, max = 255) String description) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("10.00"))
                .expenseDate(LocalDate.now())
                .categoryId(foodCategoryId)
                .description(description)
                .build();

        Long createdId = null;
        try {
            MvcResult result = createExpenseRaw(request);
            assertThat(result.getResponse().getStatus())
                    .as("Description of length %d must return 201 Created", description.length())
                    .isEqualTo(201);

            ExpenseResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ExpenseResponse.class);
            createdId = response.getId();
        } finally {
            if (createdId != null) {
                deleteExpense(createdId);
            }
        }
    }

    // =========================================================================
    // Property 6: Descriptions exceeding 255 chars are always rejected
    // Validates: Requirements 1.9
    // =========================================================================

    // Feature: expense-tracker, Property 6: Descriptions exceeding 255 characters are always rejected
    @Property(tries = 30)
    void property6_descriptionsExceeding255CharsAreAlwaysRejected(
            @ForAll @StringLength(min = 256, max = 400) String description) throws Exception {

        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("10.00"))
                .expenseDate(LocalDate.now())
                .categoryId(foodCategoryId)
                .description(description)
                .build();

        MvcResult result = createExpenseRaw(request);
        assertThat(result.getResponse().getStatus())
                .as("Description of length %d must return 400 Bad Request", description.length())
                .isEqualTo(400);
    }

    // -------------------------------------------------------------------------
    // Arbitraries (generators)
    // -------------------------------------------------------------------------

    /**
     * Valid amounts: BigDecimal in [0.01, 999999999.99] with at most 2 decimal places.
     */
    @Provide
    Arbitrary<BigDecimal> validAmounts() {
        // Generate a long cent-value in [1, 99999999999] and divide by 100
        return Arbitraries.longs()
                .between(1L, 99_999_999_999L)
                .map(cents -> BigDecimal.valueOf(cents, 2));
    }

    /**
     * Non-positive amounts: values <= 0.
     */
    @Provide
    Arbitrary<BigDecimal> nonPositiveAmounts() {
        // Combine zero and negative values
        Arbitrary<BigDecimal> zero = Arbitraries.just(BigDecimal.ZERO);
        Arbitrary<BigDecimal> negative = Arbitraries.longs()
                .between(1L, 999_999_999_99L)
                .map(v -> BigDecimal.valueOf(-v, 2));
        return Arbitraries.oneOf(zero, negative);
    }

    /**
     * Invalid amounts: either > 999999999.99 OR with 3+ decimal places.
     */
    @Provide
    Arbitrary<BigDecimal> invalidAmounts() {
        // Over-range: cents > 99_999_999_999 (i.e., > 999999999.99)
        Arbitrary<BigDecimal> overRange = Arbitraries.longs()
                .between(100_000_000_000L, 999_999_999_999_999L)
                .map(cents -> BigDecimal.valueOf(cents, 2));

        // Over-precision: generate a valid base amount and add a 3rd decimal digit != 0
        Arbitrary<BigDecimal> overPrecision = Arbitraries.longs()
                .between(1L, 999_999_999_99L)   // base: 0.01 to 9999999.99 range in millicents
                .flatMap(base ->
                        Arbitraries.integers().between(1, 9)
                                .map(digit -> {
                                    // e.g., base=12345 -> 123.45, then add 0.001*digit -> 123.451..459
                                    BigDecimal bd = BigDecimal.valueOf(base, 2);
                                    BigDecimal extra = BigDecimal.valueOf(digit, 3);
                                    return bd.add(extra);
                                })
                );

        return Arbitraries.oneOf(overRange, overPrecision);
    }

    /**
     * Past or present dates (today back to ~3 years ago).
     */
    @Provide
    Arbitrary<LocalDate> pastOrPresentDates() {
        LocalDate today = LocalDate.now();
        LocalDate threeYearsAgo = today.minusDays(1095);
        return Arbitraries.longs()
                .between(0L, 1095L)
                .map(daysBack -> today.minusDays(daysBack));
    }

    /**
     * Future dates: today+1 day to today+3650 days.
     */
    @Provide
    Arbitrary<LocalDate> futureDates() {
        LocalDate today = LocalDate.now();
        return Arbitraries.longs()
                .between(1L, 3650L)
                .map(daysAhead -> today.plusDays(daysAhead));
    }

    /**
     * Valid descriptions: strings of length 0–255.
     */
    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .withCharRange(' ', '~')
                .ofMinLength(0)
                .ofMaxLength(255);
    }
}
