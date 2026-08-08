package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.YearlyComparisonResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for the year-over-year comparison endpoint using jqwik.
 *
 * Feature: expense-tracker
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class YearComparisonPropertyTest {

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
                "--spring.datasource.url=jdbc:h2:mem:yearcompproptest;DB_CLOSE_DELAY=-1",
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
        String name = "YearComp-" + UUID.randomUUID().toString().substring(0, 8);
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
                .description("year-comp-prop-test")
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
    // Helpers — yearly comparison GET
    // -------------------------------------------------------------------------

    private YearlyComparisonResponse getYearlyComparisonOk(int year) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/expenses/comparison/yearly")
                        .param("year", String.valueOf(year)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), YearlyComparisonResponse.class);
    }

    // =========================================================================
    // Property 27: Directional indicator reflects the correct comparison direction
    // Validates: Requirements 9.7
    // =========================================================================

    // Feature: expense-tracker, Property 27: Directional indicator reflects the correct comparison direction
    @Property(tries = 15)
    void property27_directionalIndicatorReflectsComparisonDirection(
            @ForAll("validYears") int year,
            @ForAll("twoDistinctAmounts") BigDecimal[] amounts) throws Exception {

        BigDecimal specAmount = amounts[0];
        BigDecimal precAmount = amounts[1];

        // Use a mid-year date that is safely in the past for both specified year and preceding year
        LocalDate specDate = LocalDate.of(year, 6, 15);
        LocalDate precDate = LocalDate.of(year - 1, 6, 15);

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // --- Case: specAmount > precAmount → absoluteDifference must be positive ---
            ExpenseResponse specExpGreater = createExpense(specAmount.max(precAmount), specDate, categoryId);
            ExpenseResponse precExpLesser = createExpense(specAmount.min(precAmount), precDate, categoryId);
            createdIds.add(specExpGreater.getId());
            createdIds.add(precExpLesser.getId());

            if (!specAmount.equals(precAmount)) {
                YearlyComparisonResponse greaterResponse = getYearlyComparisonOk(year);

                if (greaterResponse.getSpecifiedYear().getTotal()
                        .compareTo(greaterResponse.getPrecedingYear().getTotal()) > 0) {
                    assertThat(greaterResponse.getAbsoluteDifference())
                            .as("absoluteDifference must be positive when specifiedYear > precedingYear")
                            .isGreaterThan(BigDecimal.ZERO);
                } else if (greaterResponse.getSpecifiedYear().getTotal()
                        .compareTo(greaterResponse.getPrecedingYear().getTotal()) < 0) {
                    assertThat(greaterResponse.getAbsoluteDifference())
                            .as("absoluteDifference must be negative when specifiedYear < precedingYear")
                            .isLessThan(BigDecimal.ZERO);
                } else {
                    assertThat(greaterResponse.getAbsoluteDifference())
                            .as("absoluteDifference must be zero when specifiedYear == precedingYear")
                            .isEqualByComparingTo(BigDecimal.ZERO);
                }
            }

            // Clean up the first pair before testing equal case
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            createdIds.clear();

            // --- Case: equal amounts → absoluteDifference must be zero ---
            ExpenseResponse specExpEqual = createExpense(specAmount, specDate, categoryId);
            ExpenseResponse precExpEqual = createExpense(specAmount, precDate, categoryId);
            createdIds.add(specExpEqual.getId());
            createdIds.add(precExpEqual.getId());

            YearlyComparisonResponse equalResponse = getYearlyComparisonOk(year);

            assertThat(equalResponse.getAbsoluteDifference())
                    .as("absoluteDifference must be zero when both year totals are equal")
                    .isEqualByComparingTo(BigDecimal.ZERO);

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Property 28: Year-over-year comparison totals are correct
    // Validates: Requirements 10.1
    // =========================================================================

    // Feature: expense-tracker, Property 28: Year-over-year comparison totals are correct
    @Property(tries = 15)
    void property28_yearOverYearComparisonTotalsAreCorrect(
            @ForAll("validYears") int year,
            @ForAll("twoAmountPairs") BigDecimal[] amounts) throws Exception {

        BigDecimal specAmount = amounts[0];
        BigDecimal precAmount = amounts[1];

        // Create one expense in the specified year and one in the preceding year
        LocalDate specDate = LocalDate.of(year, 7, 10);
        LocalDate precDate = LocalDate.of(year - 1, 7, 10);

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            ExpenseResponse specExp = createExpense(specAmount, specDate, categoryId);
            ExpenseResponse precExp = createExpense(precAmount, precDate, categoryId);
            createdIds.add(specExp.getId());
            createdIds.add(precExp.getId());

            YearlyComparisonResponse response = getYearlyComparisonOk(year);

            // specifiedYear.total must equal the sum of expenses in year Y
            assertThat(response.getSpecifiedYear().getTotal())
                    .as("specifiedYear.total (%s) must equal sum of year Y expenses (%s) for year=%d",
                            response.getSpecifiedYear().getTotal(), specAmount, year)
                    .isEqualByComparingTo(specAmount);

            // precedingYear.total must equal the sum of expenses in year Y-1
            assertThat(response.getPrecedingYear().getTotal())
                    .as("precedingYear.total (%s) must equal sum of year Y-1 expenses (%s) for year=%d",
                            response.getPrecedingYear().getTotal(), precAmount, year)
                    .isEqualByComparingTo(precAmount);

            // specifiedYear metadata must reflect the requested year
            assertThat(response.getSpecifiedYear().getYear())
                    .as("specifiedYear.year must equal the requested year")
                    .isEqualTo(year);

            // precedingYear metadata must reflect year - 1
            assertThat(response.getPrecedingYear().getYear())
                    .as("precedingYear.year must equal year - 1")
                    .isEqualTo(year - 1);

            // absoluteDifference must equal specifiedTotal - precedingTotal
            BigDecimal expectedDiff = specAmount.subtract(precAmount);
            assertThat(response.getAbsoluteDifference())
                    .as("absoluteDifference (%s) must equal specifiedTotal - precedingTotal (%s)",
                            response.getAbsoluteDifference(), expectedDiff)
                    .isEqualByComparingTo(expectedDiff);

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Property 29: Year-over-year per-month breakdown always has 12 entries
    //              with correct totals
    // Validates: Requirements 10.5
    // =========================================================================

    // Feature: expense-tracker, Property 29: Year-over-year per-month breakdown always has 12 entries with correct totals
    @Property(tries = 15)
    void property29_yearOverYearMonthlyBreakdownHas12EntriesWithCorrectTotals(
            @ForAll("validYears") int year,
            @ForAll("twoAmountPairs") BigDecimal[] amounts) throws Exception {

        // Create expenses in two known months of year Y
        int month1 = 3;  // March
        int month2 = 9;  // September
        BigDecimal amount1 = amounts[0];
        BigDecimal amount2 = amounts[1];

        LocalDate date1 = LocalDate.of(year, month1, 15);
        LocalDate date2 = LocalDate.of(year, month2, 15);

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            ExpenseResponse exp1 = createExpense(amount1, date1, categoryId);
            ExpenseResponse exp2 = createExpense(amount2, date2, categoryId);
            createdIds.add(exp1.getId());
            createdIds.add(exp2.getId());

            YearlyComparisonResponse response = getYearlyComparisonOk(year);

            List<YearlyComparisonResponse.MonthlyComparison> breakdown = response.getMonthlyBreakdown();

            // Breakdown must not be null
            assertThat(breakdown)
                    .as("monthlyBreakdown must not be null")
                    .isNotNull();

            // Breakdown must always have exactly 12 entries
            assertThat(breakdown)
                    .as("monthlyBreakdown must contain exactly 12 entries (one per month)")
                    .hasSize(12);

            // All 12 months must be present (1 through 12)
            List<Integer> months = breakdown.stream()
                    .map(YearlyComparisonResponse.MonthlyComparison::getMonth)
                    .sorted()
                    .toList();
            assertThat(months)
                    .as("monthlyBreakdown must contain entries for months 1 through 12")
                    .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

            // Month 3 (March) entry must show the correct specifiedYearTotal
            YearlyComparisonResponse.MonthlyComparison march = breakdown.stream()
                    .filter(m -> m.getMonth() == month1)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Month 3 (March) not found in breakdown"));

            assertThat(march.getSpecifiedYearTotal())
                    .as("month %d specifiedYearTotal (%s) must equal amount1 (%s) for year=%d",
                            month1, march.getSpecifiedYearTotal(), amount1, year)
                    .isEqualByComparingTo(amount1);

            // Month 9 (September) entry must show the correct specifiedYearTotal
            YearlyComparisonResponse.MonthlyComparison september = breakdown.stream()
                    .filter(m -> m.getMonth() == month2)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Month 9 (September) not found in breakdown"));

            assertThat(september.getSpecifiedYearTotal())
                    .as("month %d specifiedYearTotal (%s) must equal amount2 (%s) for year=%d",
                            month2, september.getSpecifiedYearTotal(), amount2, year)
                    .isEqualByComparingTo(amount2);

            // All other months (not month1 or month2) must have specifiedYearTotal of 0.00
            for (YearlyComparisonResponse.MonthlyComparison entry : breakdown) {
                if (entry.getMonth() != month1 && entry.getMonth() != month2) {
                    assertThat(entry.getSpecifiedYearTotal())
                            .as("month %d specifiedYearTotal must be 0.00 (no expenses created in this month)",
                                    entry.getMonth())
                            .isEqualByComparingTo(BigDecimal.ZERO);
                }
            }

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Arbitraries / Generators
    // =========================================================================

    /**
     * Specified years in the range 2010–2020.
     * The preceding year (Y-1) will be 2009–2019, all safely in the past.
     */
    @Provide
    Arbitrary<Integer> validYears() {
        return Arbitraries.integers().between(2010, 2020);
    }

    /** Two independent positive amounts in the range 1.00–999.99. */
    @Provide
    Arbitrary<BigDecimal[]> twoAmountPairs() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
        return amountArb.array(BigDecimal[].class).ofSize(2);
    }

    /**
     * Two distinct positive amounts in the range 1.00–999.99.
     * Guaranteed to be different so the directional test can exercise non-equal cases.
     */
    @Provide
    Arbitrary<BigDecimal[]> twoDistinctAmounts() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));
        return amountArb.array(BigDecimal[].class).ofSize(2)
                .filter(arr -> arr[0].compareTo(arr[1]) != 0);
    }
}
