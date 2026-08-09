package com.project.kiro.service;

import com.project.kiro.dto.request.ReportRequest;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import com.project.kiro.model.Expense;
import com.project.kiro.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating expense reports over a specified date range or predefined period.
 * Satisfies Requirements 11.1–11.4, 11.7.
 */
@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;

    public ReportService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Generate a report for the given request.
     * <p>
     * If {@code predefinedPeriod} is set, the start/end dates are resolved from the
     * predefined period name. Otherwise the explicit {@code startDate} and {@code endDate}
     * fields are used directly.
     * <p>
     * Satisfies Requirements 11.1–11.4, 11.7.
     *
     * @param request the report request (predefined period or explicit date range)
     * @return a fully-populated {@link ReportResponse}
     * @throws IllegalArgumentException if the period is unknown, or startDate is after endDate
     */
    @Transactional(readOnly = true)
    public ReportResponse generateReport(ReportRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // Step 1: Resolve predefined period if provided
        String predefined = request.getPredefinedPeriod();
        if (predefined != null && !predefined.isBlank()) {
            LocalDate[] range = resolvePredefinedPeriod(predefined);
            startDate = range[0];
            endDate = range[1];
        }

        // Step 2: Validate date range
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        // Step 3: Fetch all expenses in range
        List<Expense> expenses = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(startDate, endDate);

        // Step 4: Compute overall total
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 5: Per-category totals
        List<ReportResponse.CategoryTotal> categoryTotals =
                expenseRepository.findCategoryBreakdown(startDate, endDate)
                        .stream()
                        .map(row -> ReportResponse.CategoryTotal.builder()
                                .categoryName((String) row[0])
                                .total((BigDecimal) row[1])
                                .build())
                        .collect(Collectors.toList());

        // Step 6: Per-week subtotals (ISO weeks covering the range, non-zero only)
        List<ReportResponse.WeeklySubtotal> weeklySubtotals = computeWeeklySubtotals(startDate, endDate);

        // Step 7: Per-month subtotals (calendar months covering the range, non-zero only)
        List<ReportResponse.MonthlySubtotal> monthlySubtotals = computeMonthlySubtotals(startDate, endDate);

        // Step 8: Map expenses to response DTOs
        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(this::toExpenseResponse)
                .collect(Collectors.toList());

        // Step 9: Build and return the response
        return ReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .total(total)
                .categoryTotals(categoryTotals)
                .weeklySubtotals(weeklySubtotals)
                .monthlySubtotals(monthlySubtotals)
                .expenses(expenseResponses)
                .build();
    }

    /**
     * Resolve a predefined period name to a [startDate, endDate] pair.
     *
     * @param period the predefined period name (case-sensitive)
     * @return a two-element array: [startDate, endDate]
     * @throws IllegalArgumentException if the period name is not recognised
     */
    private LocalDate[] resolvePredefinedPeriod(String period) {
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "CURRENT_WEEK" -> {
                // Monday of current ISO week .. Sunday of current ISO week
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                LocalDate sunday = today.with(DayOfWeek.SUNDAY);
                yield new LocalDate[]{monday, sunday};
            }
            case "CURRENT_MONTH" -> {
                LocalDate first = today.withDayOfMonth(1);
                LocalDate last = today.withDayOfMonth(today.lengthOfMonth());
                yield new LocalDate[]{first, last};
            }
            case "CURRENT_YEAR" -> {
                LocalDate first = LocalDate.of(today.getYear(), 1, 1);
                LocalDate last = LocalDate.of(today.getYear(), 12, 31);
                yield new LocalDate[]{first, last};
            }
            case "LAST_MONTH" -> {
                LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastOfLastMonth = firstOfLastMonth.withDayOfMonth(firstOfLastMonth.lengthOfMonth());
                yield new LocalDate[]{firstOfLastMonth, lastOfLastMonth};
            }
            case "LAST_YEAR" -> {
                int lastYear = today.getYear() - 1;
                yield new LocalDate[]{LocalDate.of(lastYear, 1, 1), LocalDate.of(lastYear, 12, 31)};
            }
            default -> throw new IllegalArgumentException("Unsupported predefined period: " + period);
        };
    }

    /**
     * Iterate every ISO week that overlaps with [startDate, endDate] and compute
     * a weekly subtotal for each. Only weeks with a positive total are included.
     *
     * @param startDate inclusive start of the report range
     * @param endDate   inclusive end of the report range
     * @return list of non-zero weekly subtotals in chronological order
     */
    private List<ReportResponse.WeeklySubtotal> computeWeeklySubtotals(LocalDate startDate, LocalDate endDate) {
        List<ReportResponse.WeeklySubtotal> result = new ArrayList<>();

        // Start at the Monday of the ISO week that contains startDate
        LocalDate weekMonday = startDate.with(DayOfWeek.MONDAY);

        while (!weekMonday.isAfter(endDate)) {
            LocalDate weekSunday = weekMonday.plusDays(6);

            // Clamp the query window to the report range
            LocalDate queryStart = weekMonday.isBefore(startDate) ? startDate : weekMonday;
            LocalDate queryEnd = weekSunday.isAfter(endDate) ? endDate : weekSunday;

            BigDecimal weekTotal = expenseRepository.sumAmountByDateRange(queryStart, queryEnd);

            if (weekTotal.compareTo(BigDecimal.ZERO) > 0) {
                int isoYear = weekMonday.get(IsoFields.WEEK_BASED_YEAR);
                int isoWeek = weekMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                result.add(ReportResponse.WeeklySubtotal.builder()
                        .year(isoYear)
                        .week(isoWeek)
                        .total(weekTotal)
                        .build());
            }

            weekMonday = weekMonday.plusWeeks(1);
        }

        return result;
    }

    /**
     * Iterate every calendar month that overlaps with [startDate, endDate] and compute
     * a monthly subtotal for each. Only months with a positive total are included.
     *
     * @param startDate inclusive start of the report range
     * @param endDate   inclusive end of the report range
     * @return list of non-zero monthly subtotals in chronological order
     */
    private List<ReportResponse.MonthlySubtotal> computeMonthlySubtotals(LocalDate startDate, LocalDate endDate) {
        List<ReportResponse.MonthlySubtotal> result = new ArrayList<>();

        // Start at the first day of the month containing startDate
        LocalDate monthStart = startDate.withDayOfMonth(1);

        while (!monthStart.isAfter(endDate)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            // Clamp the query window to the report range
            LocalDate queryStart = monthStart.isBefore(startDate) ? startDate : monthStart;
            LocalDate queryEnd = monthEnd.isAfter(endDate) ? endDate : monthEnd;

            BigDecimal monthTotal = expenseRepository.sumAmountByDateRange(queryStart, queryEnd);

            if (monthTotal.compareTo(BigDecimal.ZERO) > 0) {
                result.add(ReportResponse.MonthlySubtotal.builder()
                        .year(monthStart.getYear())
                        .month(monthStart.getMonthValue())
                        .total(monthTotal)
                        .build());
            }

            monthStart = monthStart.plusMonths(1);
        }

        return result;
    }

    /**
     * Map an {@link Expense} entity to an {@link ExpenseResponse} DTO.
     *
     * @param expense the entity to map
     * @return the mapped DTO
     */
    private ExpenseResponse toExpenseResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .description(expense.getDescription())
                .transactionType(expense.getTransactionType() != null ? expense.getTransactionType() : "DEBIT")
                .build();
    }
}
