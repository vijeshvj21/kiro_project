package com.project.kiro.service;

import com.project.kiro.dto.response.ComparisonResponse;
import com.project.kiro.dto.response.YearlyComparisonResponse;
import com.project.kiro.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for period-over-period expense comparisons.
 * Satisfies Requirements 9.1–9.7, 10.1–10.5.
 */
@Service
public class ComparisonService {

    private final ExpenseRepository expenseRepository;

    public ComparisonService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Compare the specified month against the immediately preceding calendar month.
     * <p>
     * Computes totals for both months (0.00 if no expenses exist), the absolute
     * difference, and the percentage change (null + percentageChangeAvailable=false
     * when the preceding month total is 0). Also builds a per-category breakdown
     * covering every category present in either month, with 0.00 for absent months.
     * Satisfies Requirements 9.1–9.7.
     *
     * @param year  the year of the specified month
     * @param month the month number (1–12) of the specified month
     * @return a ComparisonResponse with all comparison data
     */
    @Transactional(readOnly = true)
    public ComparisonResponse compareMonths(int year, int month) {
        YearMonth specifiedYM = YearMonth.of(year, month);
        YearMonth precedingYM = specifiedYM.minusMonths(1);

        LocalDate specStart = specifiedYM.atDay(1);
        LocalDate specEnd = specifiedYM.atEndOfMonth();
        LocalDate precStart = precedingYM.atDay(1);
        LocalDate precEnd = precedingYM.atEndOfMonth();

        BigDecimal specifiedTotal = expenseRepository.sumAmountByDateRange(specStart, specEnd);
        BigDecimal precedingTotal = expenseRepository.sumAmountByDateRange(precStart, precEnd);

        BigDecimal absoluteDifference = specifiedTotal.subtract(precedingTotal);

        BigDecimal percentageChange;
        boolean percentageChangeAvailable;
        if (precedingTotal.compareTo(BigDecimal.ZERO) == 0) {
            percentageChange = null;
            percentageChangeAvailable = false;
        } else {
            percentageChange = specifiedTotal.subtract(precedingTotal)
                    .divide(precedingTotal, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            percentageChangeAvailable = true;
        }

        List<ComparisonResponse.CategoryComparison> categoryBreakdown =
                buildCategoryBreakdown(specStart, specEnd, precStart, precEnd);

        return ComparisonResponse.builder()
                .specifiedMonth(ComparisonResponse.MonthSummary.builder()
                        .year(specifiedYM.getYear())
                        .month(specifiedYM.getMonthValue())
                        .total(specifiedTotal)
                        .build())
                .precedingMonth(ComparisonResponse.MonthSummary.builder()
                        .year(precedingYM.getYear())
                        .month(precedingYM.getMonthValue())
                        .total(precedingTotal)
                        .build())
                .absoluteDifference(absoluteDifference)
                .percentageChange(percentageChange)
                .percentageChangeAvailable(percentageChangeAvailable)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    /**
     * Compare the specified year against the immediately preceding calendar year.
     * <p>
     * Computes totals for both years (0.00 if no expenses exist), the absolute
     * difference, and the percentage change (null + percentageChangeAvailable=false
     * when the preceding year total is 0). Also builds a per-month breakdown with
     * exactly 12 entries (months 1–12), with 0.00 for months with no expenses.
     * Satisfies Requirements 10.1–10.5.
     *
     * @param year the specified year
     * @return a YearlyComparisonResponse with all comparison data
     */
    @Transactional(readOnly = true)
    public YearlyComparisonResponse compareYears(int year) {
        int precedingYear = year - 1;

        LocalDate specYearStart = LocalDate.of(year, 1, 1);
        LocalDate specYearEnd = LocalDate.of(year, 12, 31);
        LocalDate precYearStart = LocalDate.of(precedingYear, 1, 1);
        LocalDate precYearEnd = LocalDate.of(precedingYear, 12, 31);

        BigDecimal specifiedTotal = expenseRepository.sumAmountByDateRange(specYearStart, specYearEnd);
        BigDecimal precedingTotal = expenseRepository.sumAmountByDateRange(precYearStart, precYearEnd);

        BigDecimal absoluteDifference = specifiedTotal.subtract(precedingTotal);

        BigDecimal percentageChange;
        boolean percentageChangeAvailable;
        if (precedingTotal.compareTo(BigDecimal.ZERO) == 0) {
            percentageChange = null;
            percentageChangeAvailable = false;
        } else {
            percentageChange = specifiedTotal.subtract(precedingTotal)
                    .divide(precedingTotal, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            percentageChangeAvailable = true;
        }

        List<YearlyComparisonResponse.MonthlyComparison> monthlyBreakdown =
                buildMonthlyBreakdown(year, precedingYear);

        return YearlyComparisonResponse.builder()
                .specifiedYear(YearlyComparisonResponse.YearSummary.builder()
                        .year(year)
                        .total(specifiedTotal)
                        .build())
                .precedingYear(YearlyComparisonResponse.YearSummary.builder()
                        .year(precedingYear)
                        .total(precedingTotal)
                        .build())
                .absoluteDifference(absoluteDifference)
                .percentageChange(percentageChange)
                .percentageChangeAvailable(percentageChangeAvailable)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    /**
     * Build a per-category breakdown covering every category present in either period.
     * Categories absent from a period receive a total of 0.00.
     *
     * @param specStart start of the specified period (inclusive)
     * @param specEnd   end of the specified period (inclusive)
     * @param precStart start of the preceding period (inclusive)
     * @param precEnd   end of the preceding period (inclusive)
     * @return list of CategoryComparison entries, one per distinct category
     */
    private List<ComparisonResponse.CategoryComparison> buildCategoryBreakdown(
            LocalDate specStart, LocalDate specEnd,
            LocalDate precStart, LocalDate precEnd) {

        Map<String, BigDecimal> specMap = toCategoryMap(
                expenseRepository.sumByCategoryAndDateRange(specStart, specEnd));
        Map<String, BigDecimal> precMap = toCategoryMap(
                expenseRepository.sumByCategoryAndDateRange(precStart, precEnd));

        // Union of all category names from both periods
        Map<String, BigDecimal[]> union = new LinkedHashMap<>();
        for (String cat : specMap.keySet()) {
            union.put(cat, new BigDecimal[]{specMap.get(cat), BigDecimal.ZERO});
        }
        for (Map.Entry<String, BigDecimal> entry : precMap.entrySet()) {
            union.computeIfAbsent(entry.getKey(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            union.get(entry.getKey())[1] = entry.getValue();
        }

        List<ComparisonResponse.CategoryComparison> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : union.entrySet()) {
            result.add(ComparisonResponse.CategoryComparison.builder()
                    .categoryName(entry.getKey())
                    .specifiedMonthTotal(entry.getValue()[0])
                    .precedingMonthTotal(entry.getValue()[1])
                    .build());
        }
        return result;
    }

    /**
     * Build a per-month breakdown for a year-over-year comparison.
     * Always returns exactly 12 entries (months 1–12), with 0.00 for months
     * that have no expenses in either year.
     *
     * @param specYear     the specified year
     * @param precedingYear the preceding year
     * @return list of 12 MonthlyComparison entries
     */
    private List<YearlyComparisonResponse.MonthlyComparison> buildMonthlyBreakdown(
            int specYear, int precedingYear) {

        List<YearlyComparisonResponse.MonthlyComparison> breakdown = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            YearMonth specYM = YearMonth.of(specYear, m);
            YearMonth precYM = YearMonth.of(precedingYear, m);

            BigDecimal specMonthTotal = expenseRepository.sumAmountByDateRange(
                    specYM.atDay(1), specYM.atEndOfMonth());
            BigDecimal precMonthTotal = expenseRepository.sumAmountByDateRange(
                    precYM.atDay(1), precYM.atEndOfMonth());

            breakdown.add(YearlyComparisonResponse.MonthlyComparison.builder()
                    .month(m)
                    .specifiedYearTotal(specMonthTotal)
                    .precedingYearTotal(precMonthTotal)
                    .build());
        }
        return breakdown;
    }

    /**
     * Convert a list of [categoryName, total] Object[] rows from a repository query
     * into a Map keyed by category name.
     *
     * @param rows query result rows, each being Object[]{String, BigDecimal}
     * @return map of category name to total amount
     */
    private Map<String, BigDecimal> toCategoryMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }
}
