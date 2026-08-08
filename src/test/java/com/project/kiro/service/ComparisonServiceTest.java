package com.project.kiro.service;

import com.project.kiro.dto.response.ComparisonResponse;
import com.project.kiro.dto.response.YearlyComparisonResponse;
import com.project.kiro.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ComparisonService.
 * Validates Requirements 9.3, 9.4, 10.3, 10.4.
 */
@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ComparisonService comparisonService;

    // -------------------------------------------------------------------------
    // compareMonths tests
    // -------------------------------------------------------------------------

    /**
     * Requirement 9.3 / 9.4 — both months have expenses; percentage change is computed.
     * specified=150, preceding=100 → percentageChange=50.00, percentageChangeAvailable=true
     */
    @Test
    void compareMonths_bothMonthsHaveExpenses_computesPercentageChange() {
        // specified month: 2024-03, preceding month: 2024-02
        LocalDate specStart = LocalDate.of(2024, 3, 1);
        LocalDate specEnd   = LocalDate.of(2024, 3, 31);
        LocalDate precStart = LocalDate.of(2024, 2, 1);
        LocalDate precEnd   = LocalDate.of(2024, 2, 29);

        when(expenseRepository.sumAmountByDateRange(specStart, specEnd))
                .thenReturn(BigDecimal.valueOf(150));
        when(expenseRepository.sumAmountByDateRange(precStart, precEnd))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumByCategoryAndDateRange(any(), any()))
                .thenReturn(Collections.emptyList());

        ComparisonResponse response = comparisonService.compareMonths(2024, 3);

        assertThat(response.getSpecifiedMonth().getTotal()).isEqualByComparingTo("150");
        assertThat(response.getPrecedingMonth().getTotal()).isEqualByComparingTo("100");
        assertThat(response.getAbsoluteDifference()).isEqualByComparingTo("50");
        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("50.00");
    }

    /**
     * Requirement 9.3 — preceding total is zero → percentageChange=null, percentageChangeAvailable=false
     */
    @Test
    void compareMonths_precedingTotalIsZero_percentageChangeUnavailable() {
        LocalDate specStart = LocalDate.of(2024, 3, 1);
        LocalDate specEnd   = LocalDate.of(2024, 3, 31);
        LocalDate precStart = LocalDate.of(2024, 2, 1);
        LocalDate precEnd   = LocalDate.of(2024, 2, 29);

        when(expenseRepository.sumAmountByDateRange(specStart, specEnd))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumAmountByDateRange(precStart, precEnd))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumByCategoryAndDateRange(any(), any()))
                .thenReturn(Collections.emptyList());

        ComparisonResponse response = comparisonService.compareMonths(2024, 3);

        assertThat(response.isPercentageChangeAvailable()).isFalse();
        assertThat(response.getPercentageChange()).isNull();
    }

    /**
     * Requirement 9.4 — equal totals → percentageChange=0.00, percentageChangeAvailable=true
     */
    @Test
    void compareMonths_equalTotals_percentageChangeIsZero() {
        LocalDate specStart = LocalDate.of(2024, 3, 1);
        LocalDate specEnd   = LocalDate.of(2024, 3, 31);
        LocalDate precStart = LocalDate.of(2024, 2, 1);
        LocalDate precEnd   = LocalDate.of(2024, 2, 29);

        when(expenseRepository.sumAmountByDateRange(specStart, specEnd))
                .thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.sumAmountByDateRange(precStart, precEnd))
                .thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.sumByCategoryAndDateRange(any(), any()))
                .thenReturn(Collections.emptyList());

        ComparisonResponse response = comparisonService.compareMonths(2024, 3);

        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("0.00");
        assertThat(response.getAbsoluteDifference()).isEqualByComparingTo("0");
    }

    /**
     * Requirement 9.4 — specified < preceding → negative percentage change.
     * specified=50, preceding=100 → percentageChange=-50.00
     */
    @Test
    void compareMonths_specifiedLessThanPreceding_negativePercentageChange() {
        LocalDate specStart = LocalDate.of(2024, 3, 1);
        LocalDate specEnd   = LocalDate.of(2024, 3, 31);
        LocalDate precStart = LocalDate.of(2024, 2, 1);
        LocalDate precEnd   = LocalDate.of(2024, 2, 29);

        when(expenseRepository.sumAmountByDateRange(specStart, specEnd))
                .thenReturn(BigDecimal.valueOf(50));
        when(expenseRepository.sumAmountByDateRange(precStart, precEnd))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumByCategoryAndDateRange(any(), any()))
                .thenReturn(Collections.emptyList());

        ComparisonResponse response = comparisonService.compareMonths(2024, 3);

        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("-50.00");
        assertThat(response.getAbsoluteDifference()).isEqualByComparingTo("-50");
    }

    /**
     * Requirement 9.5 — category breakdown: categories absent from one month get 0.00.
     * "Food" only in specified, "Transport" only in preceding.
     */
    @Test
    void compareMonths_categoryBreakdown_absentMonthGetsZero() {
        LocalDate specStart = LocalDate.of(2024, 3, 1);
        LocalDate specEnd   = LocalDate.of(2024, 3, 31);
        LocalDate precStart = LocalDate.of(2024, 2, 1);
        LocalDate precEnd   = LocalDate.of(2024, 2, 29);

        when(expenseRepository.sumAmountByDateRange(specStart, specEnd))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumAmountByDateRange(precStart, precEnd))
                .thenReturn(BigDecimal.valueOf(80));

        // "Food" present only in specified month
        List<Object[]> specCategories = Collections.singletonList(new Object[]{"Food", BigDecimal.valueOf(100)});
        when(expenseRepository.sumByCategoryAndDateRange(specStart, specEnd))
                .thenReturn(specCategories);
        // "Transport" present only in preceding month
        List<Object[]> precCategories = Collections.singletonList(new Object[]{"Transport", BigDecimal.valueOf(80)});
        when(expenseRepository.sumByCategoryAndDateRange(precStart, precEnd))
                .thenReturn(precCategories);

        ComparisonResponse response = comparisonService.compareMonths(2024, 3);

        List<ComparisonResponse.CategoryComparison> breakdown = response.getCategoryBreakdown();
        assertThat(breakdown).hasSize(2);

        ComparisonResponse.CategoryComparison food = breakdown.stream()
                .filter(c -> "Food".equals(c.getCategoryName()))
                .findFirst().orElseThrow();
        assertThat(food.getSpecifiedMonthTotal()).isEqualByComparingTo("100");
        assertThat(food.getPrecedingMonthTotal()).isEqualByComparingTo("0.00");

        ComparisonResponse.CategoryComparison transport = breakdown.stream()
                .filter(c -> "Transport".equals(c.getCategoryName()))
                .findFirst().orElseThrow();
        assertThat(transport.getSpecifiedMonthTotal()).isEqualByComparingTo("0.00");
        assertThat(transport.getPrecedingMonthTotal()).isEqualByComparingTo("80");
    }

    // -------------------------------------------------------------------------
    // compareYears tests
    // -------------------------------------------------------------------------

    /**
     * Requirement 10.3 / 10.4 — both years have expenses; percentage change is computed.
     * specified=150, preceding=100 → percentageChange=50.00, percentageChangeAvailable=true
     */
    @Test
    void compareYears_bothYearsHaveExpenses_computesPercentageChange() {
        // Stub yearly totals
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(150));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.valueOf(100));

        // Stub monthly breakdown calls for both years (12 months × 2 years = 24 calls)
        stubMonthlyTotals(2024, BigDecimal.valueOf(150));
        stubMonthlyTotals(2023, BigDecimal.valueOf(100));

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        assertThat(response.getSpecifiedYear().getTotal()).isEqualByComparingTo("150");
        assertThat(response.getPrecedingYear().getTotal()).isEqualByComparingTo("100");
        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("50.00");
    }

    /**
     * Requirement 10.3 — preceding year total is zero → percentageChange=null, percentageChangeAvailable=false
     */
    @Test
    void compareYears_precedingTotalIsZero_percentageChangeUnavailable() {
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.ZERO);

        stubMonthlyTotals(2024, BigDecimal.valueOf(100));
        stubMonthlyTotals(2023, BigDecimal.ZERO);

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        assertThat(response.isPercentageChangeAvailable()).isFalse();
        assertThat(response.getPercentageChange()).isNull();
    }

    /**
     * Requirement 10.4 — equal year totals → percentageChange=0.00, percentageChangeAvailable=true
     */
    @Test
    void compareYears_equalTotals_percentageChangeIsZero() {
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(500));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.valueOf(500));

        stubMonthlyTotals(2024, BigDecimal.valueOf(500));
        stubMonthlyTotals(2023, BigDecimal.valueOf(500));

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("0.00");
        assertThat(response.getAbsoluteDifference()).isEqualByComparingTo("0");
    }

    /**
     * Requirement 10.4 — specified < preceding → negative percentage change.
     * specified=50, preceding=100 → percentageChange=-50.00
     */
    @Test
    void compareYears_specifiedLessThanPreceding_negativePercentageChange() {
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(50));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.valueOf(100));

        stubMonthlyTotals(2024, BigDecimal.valueOf(50));
        stubMonthlyTotals(2023, BigDecimal.valueOf(100));

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        assertThat(response.isPercentageChangeAvailable()).isTrue();
        assertThat(response.getPercentageChange()).isEqualByComparingTo("-50.00");
        assertThat(response.getAbsoluteDifference()).isEqualByComparingTo("-50");
    }

    /**
     * Requirement 10.5 — monthly breakdown has exactly 12 entries.
     */
    @Test
    void compareYears_monthlyBreakdownHasExactly12Entries() {
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(1200));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.valueOf(1000));

        stubMonthlyTotals(2024, BigDecimal.valueOf(100));
        stubMonthlyTotals(2023, BigDecimal.valueOf(83));

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        assertThat(response.getMonthlyBreakdown()).hasSize(12);
    }

    /**
     * Requirement 10.5 — monthly breakdown entries have correct month numbers (1–12).
     */
    @Test
    void compareYears_monthlyBreakdownHasCorrectMonthNumbers() {
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(BigDecimal.valueOf(1200));
        when(expenseRepository.sumAmountByDateRange(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(BigDecimal.valueOf(1000));

        stubMonthlyTotals(2024, BigDecimal.valueOf(100));
        stubMonthlyTotals(2023, BigDecimal.valueOf(83));

        YearlyComparisonResponse response = comparisonService.compareYears(2024);

        List<Integer> months = response.getMonthlyBreakdown().stream()
                .map(YearlyComparisonResponse.MonthlyComparison::getMonth)
                .toList();
        assertThat(months).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Stubs every monthly date-range call for the given year to return a fixed total.
     * Uses lenient stubbing so that only the calls actually made during the test are matched.
     */
    private void stubMonthlyTotals(int year, BigDecimal total) {
        for (int month = 1; month <= 12; month++) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
            when(expenseRepository.sumAmountByDateRange(start, end)).thenReturn(total);
        }
    }
}
