package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.dto.response.ComparisonResponse;
import com.project.kiro.dto.response.ExpenseResponse;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for the month-over-month comparison endpoint using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class MonthComparisonPropertyTest {

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
                "--spring.datasource.url=jdbc:h2:mem:monthcompproptest;DB_CLOSE_DELAY=-1",
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
        String name = "MonthComp-" + UUID.randomUUID().toString().substring(0, 8);
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
                .description("month-comp-prop-test")
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
    // Helpers — comparison GET
    // -------------------------------------------------------------------------

    private ComparisonResponse getMonthlyComparisonOk(int year, int month) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/expenses/comparison/monthly")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ComparisonResponse.class);
    }

    // =========================================================================
    // Property 24: Month-over-month comparison totals are correct
    // Validates: Requirements 9.1
    // =========================================================================

    // Feature: expense-tracker, Property 24: Month-over-month comparison totals are correct
    @Property(tries = 15)
    void property24_monthOverMonthComparisonTotalsAreCorrect(
            @ForAll("validYearMonthPairs") int[] yearMonth,
            @ForAll("twoAmountPairs") BigDecimal[] amounts) throws Exception {

        int year = yearMonth[0];
        int month = yearMonth[1]; // months 2–12, so preceding month stays in same year

        YearMonth specifiedYM = YearMonth.of(year, month);
        YearMonth precedingYM = specifiedYM.minusMonths(1);

        // One expense in the specified month, one in the preceding month
        LocalDate specDate = specifiedYM.atDay(15);
        LocalDate precDate = precedingYM.atDay(15);

        BigDecimal specAmount = amounts[0];
        BigDecimal precAmount = amounts[1];

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            ExpenseResponse specExp = createExpense(specAmount, specDate, categoryId);
            ExpenseResponse precExp = createExpense(precAmount, precDate, categoryId);
            createdIds.add(specExp.getId());
            createdIds.add(precExp.getId());

            ComparisonResponse response = getMonthlyComparisonOk(year, month);

            // specifiedMonth.total must equal the sum of expenses in month M
            assertThat(response.getSpecifiedMonth().getTotal())
                    .as("specifiedMonth.total (%s) must equal sum of M expenses (%s) for year=%d month=%d",
                            response.getSpecifiedMonth().getTotal(), specAmount, year, month)
                    .isEqualByComparingTo(specAmount);

            // precedingMonth.total must equal the sum of expenses in month M-1
            assertThat(response.getPrecedingMonth().getTotal())
                    .as("precedingMonth.total (%s) must equal sum of M-1 expenses (%s) for year=%d month=%d",
                            response.getPrecedingMonth().getTotal(), precAmount, year, month)
                    .isEqualByComparingTo(precAmount);

            // specifiedMonth metadata must reflect the requested year/month
            assertThat(response.getSpecifiedMonth().getYear()).isEqualTo(year);
            assertThat(response.getSpecifiedMonth().getMonth()).isEqualTo(month);

            // precedingMonth metadata must reflect the preceding year/month
            assertThat(response.getPrecedingMonth().getYear()).isEqualTo(precedingYM.getYear());
            assertThat(response.getPrecedingMonth().getMonth()).isEqualTo(precedingYM.getMonthValue());

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Property 25: Percentage change formula is correct when preceding period
    //              total is non-zero; zero preceding → percentageChangeAvailable=false
    // Validates: Requirements 9.3, 10.3
    // =========================================================================

    // Feature: expense-tracker, Property 25: Percentage change formula is correct when preceding period total is non-zero
    @Property(tries = 15)
    void property25_percentageChangeFormulaIsCorrect(
            @ForAll("validYearMonthPairs") int[] yearMonth,
            @ForAll("twoAmountPairs") BigDecimal[] amounts) throws Exception {

        int year = yearMonth[0];
        int month = yearMonth[1]; // months 2–12

        YearMonth specifiedYM = YearMonth.of(year, month);
        YearMonth precedingYM = specifiedYM.minusMonths(1);

        LocalDate specDate = specifiedYM.atDay(10);
        LocalDate precDate = precedingYM.atDay(10);

        BigDecimal specAmount = amounts[0];
        BigDecimal precAmount = amounts[1];

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // --- Case 1: non-zero preceding month → percentageChange must be computed correctly ---
            ExpenseResponse specExp = createExpense(specAmount, specDate, categoryId);
            ExpenseResponse precExp = createExpense(precAmount, precDate, categoryId);
            createdIds.add(specExp.getId());
            createdIds.add(precExp.getId());

            ComparisonResponse response = getMonthlyComparisonOk(year, month);

            BigDecimal expectedPct = specAmount.subtract(precAmount)
                    .divide(precAmount, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            assertThat(response.isPercentageChangeAvailable())
                    .as("percentageChangeAvailable must be true when preceding total is non-zero")
                    .isTrue();

            assertThat(response.getPercentageChange())
                    .as("percentageChange (%s) must equal ((specified - preceding) / preceding) * 100 = %s",
                            response.getPercentageChange(), expectedPct)
                    .isEqualByComparingTo(expectedPct);

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // Feature: expense-tracker, Property 25b: Zero preceding total → percentageChangeAvailable=false
    @Property(tries = 15)
    void property25b_zeroPrecedingTotalMakesPercentageChangeUnavailable(
            @ForAll("validYearMonthPairs") int[] yearMonth,
            @ForAll("singleAmount") BigDecimal specAmount) throws Exception {

        int year = yearMonth[0];
        int month = yearMonth[1]; // months 2–12

        YearMonth specifiedYM = YearMonth.of(year, month);
        LocalDate specDate = specifiedYM.atDay(20);

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Only create an expense in the specified month — preceding month has no expenses
            ExpenseResponse specExp = createExpense(specAmount, specDate, categoryId);
            createdIds.add(specExp.getId());

            ComparisonResponse response = getMonthlyComparisonOk(year, month);

            assertThat(response.getPrecedingMonth().getTotal())
                    .as("precedingMonth.total must be 0.00 when no expenses exist in that month")
                    .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(response.isPercentageChangeAvailable())
                    .as("percentageChangeAvailable must be false when preceding total is zero")
                    .isFalse();

            assertThat(response.getPercentageChange())
                    .as("percentageChange must be null when preceding total is zero")
                    .isNull();

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Property 26: Month-over-month per-category breakdown is correct
    // Validates: Requirements 9.5
    // =========================================================================

    // Feature: expense-tracker, Property 26: Month-over-month per-category breakdown is correct
    @Property(tries = 15)
    void property26_perCategoryBreakdownIsCorrect(
            @ForAll("validYearMonthPairs") int[] yearMonth,
            @ForAll("fourAmountQuad") BigDecimal[] amounts) throws Exception {

        int year = yearMonth[0];
        int month = yearMonth[1]; // months 2–12

        YearMonth specifiedYM = YearMonth.of(year, month);
        YearMonth precedingYM = specifiedYM.minusMonths(1);

        LocalDate specDate = specifiedYM.atDay(8);
        LocalDate precDate = precedingYM.atDay(8);

        // amounts[0] = cat1 in specified month
        // amounts[1] = cat1 in preceding month
        // amounts[2] = cat2 in specified month
        // amounts[3] = cat2 in preceding month
        BigDecimal cat1SpecAmount = amounts[0];
        BigDecimal cat1PrecAmount = amounts[1];
        BigDecimal cat2SpecAmount = amounts[2];
        BigDecimal cat2PrecAmount = amounts[3];

        CategoryResponse cat1 = createCategory();
        CategoryResponse cat2 = createCategory();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create expenses in cat1 for both months
            ExpenseResponse cat1SpecExp = createExpense(cat1SpecAmount, specDate, cat1.getId());
            ExpenseResponse cat1PrecExp = createExpense(cat1PrecAmount, precDate, cat1.getId());
            createdIds.add(cat1SpecExp.getId());
            createdIds.add(cat1PrecExp.getId());

            // Create expenses in cat2 for both months
            ExpenseResponse cat2SpecExp = createExpense(cat2SpecAmount, specDate, cat2.getId());
            ExpenseResponse cat2PrecExp = createExpense(cat2PrecAmount, precDate, cat2.getId());
            createdIds.add(cat2SpecExp.getId());
            createdIds.add(cat2PrecExp.getId());

            ComparisonResponse response = getMonthlyComparisonOk(year, month);

            List<ComparisonResponse.CategoryComparison> breakdown = response.getCategoryBreakdown();
            assertThat(breakdown).as("categoryBreakdown must not be null").isNotNull();

            // Both category names must appear in the breakdown
            List<String> categoryNames = breakdown.stream()
                    .map(ComparisonResponse.CategoryComparison::getCategoryName)
                    .toList();
            assertThat(categoryNames)
                    .as("categoryBreakdown must contain category '%s'", cat1.getName())
                    .contains(cat1.getName());
            assertThat(categoryNames)
                    .as("categoryBreakdown must contain category '%s'", cat2.getName())
                    .contains(cat2.getName());

            // Verify cat1 totals per month
            ComparisonResponse.CategoryComparison cat1Entry = breakdown.stream()
                    .filter(c -> c.getCategoryName().equals(cat1.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("cat1 not found in breakdown"));

            assertThat(cat1Entry.getSpecifiedMonthTotal())
                    .as("cat1 specifiedMonthTotal (%s) must equal cat1 specified amount (%s)",
                            cat1Entry.getSpecifiedMonthTotal(), cat1SpecAmount)
                    .isEqualByComparingTo(cat1SpecAmount);

            assertThat(cat1Entry.getPrecedingMonthTotal())
                    .as("cat1 precedingMonthTotal (%s) must equal cat1 preceding amount (%s)",
                            cat1Entry.getPrecedingMonthTotal(), cat1PrecAmount)
                    .isEqualByComparingTo(cat1PrecAmount);

            // Verify cat2 totals per month
            ComparisonResponse.CategoryComparison cat2Entry = breakdown.stream()
                    .filter(c -> c.getCategoryName().equals(cat2.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("cat2 not found in breakdown"));

            assertThat(cat2Entry.getSpecifiedMonthTotal())
                    .as("cat2 specifiedMonthTotal (%s) must equal cat2 specified amount (%s)",
                            cat2Entry.getSpecifiedMonthTotal(), cat2SpecAmount)
                    .isEqualByComparingTo(cat2SpecAmount);

            assertThat(cat2Entry.getPrecedingMonthTotal())
                    .as("cat2 precedingMonthTotal (%s) must equal cat2 preceding amount (%s)",
                            cat2Entry.getPrecedingMonthTotal(), cat2PrecAmount)
                    .isEqualByComparingTo(cat2PrecAmount);

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(cat1.getId()); } catch (Exception ignored) {}
            try { categoryRepository.deleteById(cat2.getId()); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Arbitraries / Generators
    // =========================================================================

    /**
     * Year 2015–2022, month 2–12 so that month M-1 stays in the same year
     * (avoids year-boundary complexity).
     */
    @Provide
    Arbitrary<int[]> validYearMonthPairs() {
        return Arbitraries.integers().between(2015, 2022)
                .flatMap(year -> Arbitraries.integers().between(2, 12)
                        .map(month -> new int[]{year, month}));
    }

    /** Two independent positive amounts in the range 1.00–999.99. */
    @Provide
    Arbitrary<BigDecimal[]> twoAmountPairs() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
        return amountArb.array(BigDecimal[].class).ofSize(2);
    }

    /** A single positive amount in the range 1.00–999.99. */
    @Provide
    Arbitrary<BigDecimal> singleAmount() {
        return Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
    }

    /** Four independent positive amounts for the two-category breakdown test. */
    @Provide
    Arbitrary<BigDecimal[]> fourAmountQuad() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
        return amountArb.array(BigDecimal[].class).ofSize(4);
    }
}
