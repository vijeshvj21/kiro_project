package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.MonthlyExpenseResponse;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for the monthly expense view using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class MonthlyViewPropertyTest {

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
                "--spring.datasource.url=jdbc:h2:mem:monthlyproptest;DB_CLOSE_DELAY=-1",
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
        String name = "MonthlyTest-" + UUID.randomUUID().toString().substring(0, 8);
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
                .description("monthly-prop-test")
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
    // Helpers — monthly GET
    // -------------------------------------------------------------------------

    private MvcResult getMonthlyRaw(int year, int month) throws Exception {
        return mockMvc.perform(get("/api/v1/expenses/monthly")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andReturn();
    }

    private MonthlyExpenseResponse getMonthlyOk(int year, int month) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/expenses/monthly")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MonthlyExpenseResponse.class);
    }

    // =========================================================================
    // Property 14: Monthly view returns only expenses within the requested month,
    //              sorted descending
    // Validates: Requirements 4.1
    // =========================================================================

    // Feature: expense-tracker, Property 14: Monthly view returns only expenses within the requested month, sorted descending
    @Property(tries = 20)
    void property14_monthlyViewReturnsOnlyExpensesInRequestedMonthSortedDescending(
            @ForAll("validYearMonthPairs") int[] yearMonth) throws Exception {

        int year = yearMonth[0];
        int month = yearMonth[1];

        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();
        LocalDate midDay = ym.atDay(Math.max(1, ym.lengthOfMonth() / 2));

        // Adjacent month date (must be outside the target month)
        YearMonth prevYm = ym.minusMonths(1);
        LocalDate adjacentDate = prevYm.atDay(15);

        // Guard: all dates must be past-or-present (required by @PastOrPresent validation)
        LocalDate today = LocalDate.now();
        if (lastDay.isAfter(today) || adjacentDate.isAfter(today)) {
            return; // skip this try if any date would fail validation
        }

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create two expenses within the target month
            ExpenseResponse firstDayExp = createExpense(new BigDecimal("15.00"), firstDay, categoryId);
            ExpenseResponse midDayExp = createExpense(new BigDecimal("25.00"), midDay, categoryId);
            createdIds.add(firstDayExp.getId());
            createdIds.add(midDayExp.getId());

            // Create one expense in the adjacent (previous) month — must NOT appear
            ExpenseResponse outsideExp = createExpense(new BigDecimal("99.00"), adjacentDate, categoryId);
            createdIds.add(outsideExp.getId());

            MonthlyExpenseResponse response = getMonthlyOk(year, month);

            // All returned expenses must fall within [firstDay, lastDay]
            for (ExpenseResponse expense : response.getExpenses()) {
                assertThat(expense.getExpenseDate())
                        .as("Expense date %s must be within [%s, %s]",
                                expense.getExpenseDate(), firstDay, lastDay)
                        .isAfterOrEqualTo(firstDay)
                        .isBeforeOrEqualTo(lastDay);
            }

            // The outside expense must not appear
            boolean outsideFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(outsideExp.getId()));
            assertThat(outsideFound)
                    .as("Expense from adjacent month (id=%d, date=%s) must not appear in %d/%d response",
                            outsideExp.getId(), adjacentDate, year, month)
                    .isFalse();

            // The two in-month expenses must appear
            boolean firstDayFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(firstDayExp.getId()));
            boolean midDayFound = response.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(midDayExp.getId()));
            assertThat(firstDayFound).as("First-day expense must appear in monthly response").isTrue();
            assertThat(midDayFound).as("Mid-month expense must appear in monthly response").isTrue();

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
    Arbitrary<int[]> validYearMonthPairs() {
        // Use years 2015–2023 so all generated dates are safely in the past
        return Arbitraries.integers().between(2015, 2023)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .map(month -> new int[]{year, month}));
    }

    // =========================================================================
    // Property 15: Invalid month parameters are always rejected
    // Validates: Requirements 4.3
    // =========================================================================

    // Feature: expense-tracker, Property 15: Invalid month parameters are always rejected
    @Property(tries = 20)
    void property15_invalidMonthParametersAreAlwaysRejected() throws Exception {
        // month = 0 (below minimum)
        assertThat(getMonthlyRaw(2020, 0).getResponse().getStatus())
                .as("month=0 must return 400")
                .isEqualTo(400);

        // month = 13 (above maximum)
        assertThat(getMonthlyRaw(2020, 13).getResponse().getStatus())
                .as("month=13 must return 400")
                .isEqualTo(400);

        // year = 1800 (below minimum 1900)
        assertThat(getMonthlyRaw(1800, 6).getResponse().getStatus())
                .as("year=1800 must return 400")
                .isEqualTo(400);

        // year = 2200 (above maximum 2100)
        assertThat(getMonthlyRaw(2200, 6).getResponse().getStatus())
                .as("year=2200 must return 400")
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 16: Monthly response total and per-category breakdown are
    //              mathematically consistent
    // Validates: Requirements 4.4, 4.5
    // =========================================================================

    // Feature: expense-tracker, Property 16: Monthly response total and per-category breakdown are mathematically consistent
    @Property(tries = 20)
    void property16_monthlyTotalAndCategoryBreakdownAreMathematicallyConsistent(
            @ForAll("twoAmountPairs") BigDecimal[] amounts) throws Exception {

        // Use a fixed known month safely in the past: 2019, month 3 (March)
        // Rotate the month by amounts to avoid cross-test contamination across tries
        int baseMonth = (int) (amounts[0].longValue() % 6);
        int year = 2019;
        int month = 3 + baseMonth; // months 3–8 (March–August), all in 2019 → safely in the past

        YearMonth ym = YearMonth.of(year, month);
        LocalDate day1 = ym.atDay(5);
        LocalDate day2 = ym.atDay(20);

        CategoryResponse cat1 = createCategory();
        CategoryResponse cat2 = createCategory();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create expenses in two different categories in the same month
            ExpenseResponse exp1 = createExpense(amounts[0], day1, cat1.getId());
            ExpenseResponse exp2 = createExpense(amounts[1], day2, cat2.getId());
            createdIds.add(exp1.getId());
            createdIds.add(exp2.getId());

            MonthlyExpenseResponse response = getMonthlyOk(year, month);

            // --- Assertion 1: total == sum of expense amounts in the response ---
            BigDecimal sumFromExpenses = response.getExpenses().stream()
                    .map(ExpenseResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(response.getTotal())
                    .as("total (%s) must equal sum of all returned expense amounts (%s) for %d/%d",
                            response.getTotal(), sumFromExpenses, year, month)
                    .isEqualByComparingTo(sumFromExpenses);

            // --- Assertion 2: sum of categoryBreakdown totals == overall total ---
            BigDecimal sumFromBreakdown = response.getCategoryBreakdown().stream()
                    .map(MonthlyExpenseResponse.CategoryTotal::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(sumFromBreakdown)
                    .as("sum of categoryBreakdown totals (%s) must equal overall total (%s) for %d/%d",
                            sumFromBreakdown, response.getTotal(), year, month)
                    .isEqualByComparingTo(response.getTotal());

            // --- Assertion 3: each category's breakdown total == sum of its expenses ---
            Map<String, BigDecimal> expensesByCategory = response.getExpenses().stream()
                    .collect(Collectors.groupingBy(
                            ExpenseResponse::getCategoryName,
                            Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

            for (MonthlyExpenseResponse.CategoryTotal ct : response.getCategoryBreakdown()) {
                BigDecimal expectedCategoryTotal = expensesByCategory
                        .getOrDefault(ct.getCategoryName(), BigDecimal.ZERO);
                assertThat(ct.getTotal())
                        .as("categoryBreakdown total for '%s' (%s) must equal sum of its expenses (%s)",
                                ct.getCategoryName(), ct.getTotal(), expectedCategoryTotal)
                        .isEqualByComparingTo(expectedCategoryTotal);
            }

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(cat1.getId()); } catch (Exception ignored) {}
            try { categoryRepository.deleteById(cat2.getId()); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<BigDecimal[]> twoAmountPairs() {
        // Generate pairs of amounts between 1.00 and 999.99
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
        return amountArb.array(BigDecimal[].class).ofSize(2);
    }
}
