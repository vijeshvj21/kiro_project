package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.WeeklyExpenseResponse;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for the weekly expense view using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class WeeklyViewPropertyTest {

    // -------------------------------------------------------------------------
    // Spring context — started once for the entire test class (separate H2 DB)
    // -------------------------------------------------------------------------

    private static final ConfigurableApplicationContext ctx;
    private static final MockMvc mockMvc;
    private static final ObjectMapper objectMapper;
    private static final CategoryRepository categoryRepository;
    private static final ExpenseRepository expenseRepository;

    static {
        ctx = SpringApplication.run(KiroApplication.class,
                "--spring.datasource.url=jdbc:h2:mem:weeklyproptest;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.h2.console.enabled=false",
                "--server.port=0");

        mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
        objectMapper = ctx.getBean(ObjectMapper.class);
        categoryRepository = ctx.getBean(CategoryRepository.class);
        expenseRepository = ctx.getBean(ExpenseRepository.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — category creation
    // -------------------------------------------------------------------------

    private CategoryResponse createCategory() throws Exception {
        String name = "WeeklyTest-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — expense creation via REST
    // -------------------------------------------------------------------------

    private ExpenseResponse createExpense(BigDecimal amount, LocalDate date, Long categoryId) throws Exception {
        ExpenseRequest req = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(date)
                .categoryId(categoryId)
                .description("weekly-prop-test")
                .build();
        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ExpenseResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — expense deletion
    // -------------------------------------------------------------------------

    private void deleteExpense(Long id) throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/" + id))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // Helpers — weekly GET
    // -------------------------------------------------------------------------

    private MvcResult getWeeklyRaw(int year, int week) throws Exception {
        return mockMvc.perform(get("/api/v1/expenses/weekly")
                        .param("year", String.valueOf(year))
                        .param("week", String.valueOf(week)))
                .andReturn();
    }

    private WeeklyExpenseResponse getWeeklyOk(int year, int week) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/expenses/weekly")
                        .param("year", String.valueOf(year))
                        .param("week", String.valueOf(week)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), WeeklyExpenseResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — ISO week range computation
    // -------------------------------------------------------------------------

    /** Returns the Monday of the given ISO year/week. */
    private LocalDate weekStart(int year, int week) {
        return LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);
    }

    // =========================================================================
    // Property 11: Weekly view returns only expenses within the requested week,
    //              sorted descending
    // Validates: Requirements 3.1
    // =========================================================================

    // Feature: expense-tracker, Property 11: Weekly view returns only expenses within the requested week, sorted descending
    @Property(tries = 20)
    void property11_weeklyViewReturnsOnlyExpensesInRequestedWeekSortedDescending(
            @ForAll("validYearWeekPairs") int[] yearWeek) throws Exception {

        int year = yearWeek[0];
        int week = yearWeek[1];

        LocalDate monday = weekStart(year, week);
        LocalDate friday = monday.plusDays(4);   // within the week
        LocalDate adjacentMonday = monday.minusDays(7); // previous week — outside

        // Guard: ensure all dates are past-or-present (required by @PastOrPresent validation)
        LocalDate today = LocalDate.now();
        if (friday.isAfter(today) || adjacentMonday.isAfter(today)) {
            return; // skip this try if any date would fail validation
        }

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create two expenses in the target week (Monday and Friday)
            ExpenseResponse mondayExp = createExpense(new BigDecimal("10.00"), monday, categoryId);
            ExpenseResponse fridayExp = createExpense(new BigDecimal("20.00"), friday, categoryId);
            createdIds.add(mondayExp.getId());
            createdIds.add(fridayExp.getId());

            // Create one expense in the adjacent (previous) week — must NOT appear
            ExpenseResponse outsideExp = createExpense(new BigDecimal("99.00"), adjacentMonday, categoryId);
            createdIds.add(outsideExp.getId());

            WeeklyExpenseResponse response = getWeeklyOk(year, week);

            // All returned expenses must fall within the week [monday, monday+6]
            LocalDate weekEnd = monday.plusDays(6);
            for (ExpenseResponse expense : response.getExpenses()) {
                assertThat(expense.getExpenseDate())
                        .as("Expense date %s must be within [%s, %s]", expense.getExpenseDate(), monday, weekEnd)
                        .isAfterOrEqualTo(monday)
                        .isBeforeOrEqualTo(weekEnd);
            }

            // The outside expense must not appear
            boolean outsideFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(outsideExp.getId()));
            assertThat(outsideFound)
                    .as("Expense from adjacent week (id=%d, date=%s) must not appear in week %d/%d response",
                            outsideExp.getId(), adjacentMonday, year, week)
                    .isFalse();

            // The two in-week expenses must appear
            boolean mondayFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(mondayExp.getId()));
            boolean fridayFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(fridayExp.getId()));
            assertThat(mondayFound).as("Monday expense must appear in weekly response").isTrue();
            assertThat(fridayFound).as("Friday expense must appear in weekly response").isTrue();

            // Expenses must be sorted by date descending
            List<ExpenseResponse> expenses = response.getExpenses();
            for (int i = 0; i < expenses.size() - 1; i++) {
                LocalDate current = expenses.get(i).getExpenseDate();
                LocalDate next = expenses.get(i + 1).getExpenseDate();
                assertThat(current)
                        .as("Expense at index %d (date=%s) must be >= expense at index %d (date=%s)",
                                i, current, i + 1, next)
                        .isAfterOrEqualTo(next);
            }
        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<int[]> validYearWeekPairs() {
        // Use years 2000–2024 so all generated dates are safely in the past
        return Arbitraries.integers().between(2000, 2024)
                .flatMap(year -> Arbitraries.integers().between(1, 52)
                        .map(week -> new int[]{year, week}));
    }

    // =========================================================================
    // Property 12: Invalid week parameters are always rejected
    // Validates: Requirements 3.3
    // =========================================================================

    // Feature: expense-tracker, Property 12: Invalid week parameters are always rejected
    @Property(tries = 20)
    void property12_invalidWeekParametersAreAlwaysRejected() throws Exception {
        // week = 0 (below minimum)
        assertThat(getWeeklyRaw(2024, 0).getResponse().getStatus())
                .as("week=0 must return 400")
                .isEqualTo(400);

        // week = 54 (above maximum)
        assertThat(getWeeklyRaw(2024, 54).getResponse().getStatus())
                .as("week=54 must return 400")
                .isEqualTo(400);

        // year = 1800 (below minimum 1900)
        assertThat(getWeeklyRaw(1800, 1).getResponse().getStatus())
                .as("year=1800 must return 400")
                .isEqualTo(400);

        // year = 2200 (above maximum 2100)
        assertThat(getWeeklyRaw(2200, 1).getResponse().getStatus())
                .as("year=2200 must return 400")
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 13: Weekly response total equals the sum of returned expense amounts
    // Validates: Requirements 3.4
    // =========================================================================

    // Feature: expense-tracker, Property 13: Weekly response total equals the sum of returned expense amounts
    @Property(tries = 20)
    void property13_weeklyResponseTotalEqualsExpensesSum(
            @ForAll("expenseCountAndAmounts") Object[] params) throws Exception {

        int expenseCount = (int) params[0];
        @SuppressWarnings("unchecked")
        List<BigDecimal> amounts = (List<BigDecimal>) params[1];

        // Use a fixed known week safely in the past: 2020, week 10 (Mon=2020-03-02, Sun=2020-03-08)
        // Rotate the week by expenseCount to avoid cross-test contamination across tries
        int year = 2020;
        int week = 10 + (expenseCount % 20); // weeks 10–29, all safely in the past

        LocalDate monday = weekStart(year, week);

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create expenses spread across Mon–Wed of the target week
            for (int i = 0; i < expenseCount; i++) {
                LocalDate date = monday.plusDays(i % 3); // day 0, 1, or 2 (Mon/Tue/Wed)
                ExpenseResponse exp = createExpense(amounts.get(i), date, categoryId);
                createdIds.add(exp.getId());
            }

            WeeklyExpenseResponse response = getWeeklyOk(year, week);

            // Compute sum of amounts in the response
            BigDecimal sumFromExpenses = response.getExpenses().stream()
                    .map(ExpenseResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(response.getTotal())
                    .as("total field (%s) must equal sum of expense amounts (%s) for year=%d week=%d",
                            response.getTotal(), sumFromExpenses, year, week)
                    .isEqualByComparingTo(sumFromExpenses);

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<Object[]> expenseCountAndAmounts() {
        // Generate 1–3 expenses with amounts between 1.00 and 999.99
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));

        return Arbitraries.integers().between(1, 3)
                .flatMap(count -> amountArb.list().ofSize(count)
                        .map(amtList -> new Object[]{count, amtList}));
    }
}
