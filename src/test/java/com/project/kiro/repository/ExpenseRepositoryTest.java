package com.project.kiro.repository;

import com.project.kiro.model.Category;
import com.project.kiro.model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for {@link ExpenseRepository}.
 *
 * Uses @SpringBootTest with @Transactional so each test runs in a transaction
 * that is rolled back after the test, keeping the database clean.
 *
 * Validates Requirements 3.1, 4.1, 4.5
 */
@SpringBootTest
@Transactional
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category food;
    private Category transport;

    @BeforeEach
    void setUp() {
        // DataInitializer seeds default categories on startup; find or create Food/Transport
        food = categoryRepository.findByNameLower("food")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Food")
                        .nameLower("food")
                        .build()));

        transport = categoryRepository.findByNameLower("transport")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Transport")
                        .nameLower("transport")
                        .build()));
    }

    // -------------------------------------------------------------------------
    // Weekly filter tests — Requirement 3.1
    // -------------------------------------------------------------------------

    /**
     * Insert expenses spanning two different ISO weeks.
     * Query one week and assert only that week's expenses are returned.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void weeklyFilter_returnsOnlyExpensesInRequestedIsoWeek() {
        // ISO week 2 of 2025: Mon 2025-01-06 .. Sun 2025-01-12
        LocalDate week2Start = LocalDate.of(2025, 1, 6);
        LocalDate week2End   = LocalDate.of(2025, 1, 12);

        // ISO week 3 of 2025: Mon 2025-01-13 .. Sun 2025-01-19
        // (used only to place an out-of-range expense)

        // Expenses in week 2
        Expense w2e1 = expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("10.00"))
                .expenseDate(LocalDate.of(2025, 1, 6))   // Monday of week 2
                .category(food)
                .description("Week 2 Monday")
                .build());

        Expense w2e2 = expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("20.00"))
                .expenseDate(LocalDate.of(2025, 1, 12))  // Sunday of week 2
                .category(transport)
                .description("Week 2 Sunday")
                .build());

        // Expense in week 3 — should NOT appear in week 2 query
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("99.00"))
                .expenseDate(LocalDate.of(2025, 1, 13))  // Monday of week 3
                .category(food)
                .description("Week 3 Monday")
                .build());

        List<Expense> result = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(week2Start, week2End);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Expense::getId)
                .containsExactlyInAnyOrder(w2e1.getId(), w2e2.getId());

        // Verify sorted descending by date
        assertThat(result.get(0).getExpenseDate())
                .isAfterOrEqualTo(result.get(1).getExpenseDate());
    }

    /**
     * Querying a week that has no expenses returns an empty list.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void weeklyFilter_returnsEmptyList_whenNoExpensesInWeek() {
        // Insert expense in week 2 of 2025
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("15.00"))
                .expenseDate(LocalDate.of(2025, 1, 8))
                .category(food)
                .build());

        // Query week 5 of 2025 — no expenses there
        LocalDate week5Start = LocalDate.of(2025, 1, 27);
        LocalDate week5End   = LocalDate.of(2025, 2, 2);

        List<Expense> result = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(week5Start, week5End);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Monthly filter tests — Requirement 4.1
    // -------------------------------------------------------------------------

    /**
     * Insert expenses in two different months.
     * Query one month and assert only that month's expenses are returned.
     *
     * Validates: Requirements 4.1
     */
    @Test
    void monthlyFilter_returnsOnlyExpensesInRequestedCalendarMonth() {
        LocalDate janStart = LocalDate.of(2025, 1, 1);
        LocalDate janEnd   = LocalDate.of(2025, 1, 31);

        // January expenses
        Expense jan1 = expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("50.00"))
                .expenseDate(LocalDate.of(2025, 1, 5))
                .category(food)
                .description("Jan expense 1")
                .build());

        Expense jan2 = expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("30.00"))
                .expenseDate(LocalDate.of(2025, 1, 20))
                .category(transport)
                .description("Jan expense 2")
                .build());

        // February expense — should NOT appear in January query
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("75.00"))
                .expenseDate(LocalDate.of(2025, 2, 1))
                .category(food)
                .description("Feb expense")
                .build());

        List<Expense> result = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(janStart, janEnd);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Expense::getId)
                .containsExactlyInAnyOrder(jan1.getId(), jan2.getId());

        // Verify sorted descending by date
        assertThat(result.get(0).getExpenseDate())
                .isAfterOrEqualTo(result.get(1).getExpenseDate());
    }

    /**
     * Querying a month that has no expenses returns an empty list.
     *
     * Validates: Requirements 4.1
     */
    @Test
    void monthlyFilter_returnsEmptyList_whenNoExpensesInMonth() {
        // Insert expense in January 2025
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("40.00"))
                .expenseDate(LocalDate.of(2025, 1, 10))
                .category(food)
                .build());

        // Query March 2025 — no expenses there
        LocalDate marchStart = LocalDate.of(2025, 3, 1);
        LocalDate marchEnd   = LocalDate.of(2025, 3, 31);

        List<Expense> result = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(marchStart, marchEnd);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Category breakdown aggregation tests — Requirement 4.5
    // -------------------------------------------------------------------------

    /**
     * Insert expenses across multiple categories within a month.
     * Assert findCategoryBreakdown returns correct per-category sums.
     *
     * Validates: Requirements 4.5
     */
    @Test
    void categoryBreakdown_sumsCorrectlyPerCategory() {
        LocalDate start = LocalDate.of(2025, 3, 1);
        LocalDate end   = LocalDate.of(2025, 3, 31);

        // Food: 100 + 50 = 150
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("100.00"))
                .expenseDate(LocalDate.of(2025, 3, 5))
                .category(food)
                .build());
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("50.00"))
                .expenseDate(LocalDate.of(2025, 3, 15))
                .category(food)
                .build());

        // Transport: 200
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("200.00"))
                .expenseDate(LocalDate.of(2025, 3, 10))
                .category(transport)
                .build());

        // Expense outside the range — should not affect breakdown
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("999.00"))
                .expenseDate(LocalDate.of(2025, 4, 1))
                .category(food)
                .build());

        List<Object[]> breakdown = expenseRepository.findCategoryBreakdown(start, end);

        // Results are ordered by category name ascending: Food, Transport
        assertThat(breakdown).hasSizeGreaterThanOrEqualTo(2);

        // Find Food and Transport rows by name
        Object[] foodRow = breakdown.stream()
                .filter(row -> "Food".equals(row[0]))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Food row not found in breakdown"));

        Object[] transportRow = breakdown.stream()
                .filter(row -> "Transport".equals(row[0]))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Transport row not found in breakdown"));

        assertThat(((BigDecimal) foodRow[1]).compareTo(new BigDecimal("150.00"))).isZero();
        assertThat(((BigDecimal) transportRow[1]).compareTo(new BigDecimal("200.00"))).isZero();
    }

    /**
     * Category breakdown returns empty list when no expenses exist in the range.
     *
     * Validates: Requirements 4.5
     */
    @Test
    void categoryBreakdown_returnsEmpty_whenNoExpensesInRange() {
        LocalDate start = LocalDate.of(2020, 6, 1);
        LocalDate end   = LocalDate.of(2020, 6, 30);

        List<Object[]> breakdown = expenseRepository.findCategoryBreakdown(start, end);

        assertThat(breakdown).isEmpty();
    }

    /**
     * sumAmountByDateRange returns 0 when no expenses exist in the range.
     *
     * Validates: Requirements 3.4, 4.4
     */
    @Test
    void sumAmountByDateRange_returnsZero_whenNoExpenses() {
        LocalDate start = LocalDate.of(2020, 5, 1);
        LocalDate end   = LocalDate.of(2020, 5, 31);

        BigDecimal total = expenseRepository.sumAmountByDateRange(start, end);

        assertThat(total.compareTo(BigDecimal.ZERO)).isZero();
    }

    /**
     * sumAmountByDateRange returns the correct sum for expenses in the range.
     *
     * Validates: Requirements 3.4, 4.4
     */
    @Test
    void sumAmountByDateRange_returnsCorrectSum() {
        LocalDate start = LocalDate.of(2025, 7, 1);
        LocalDate end   = LocalDate.of(2025, 7, 31);

        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("100.50"))
                .expenseDate(LocalDate.of(2025, 7, 10))
                .category(food)
                .build());
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("49.50"))
                .expenseDate(LocalDate.of(2025, 7, 20))
                .category(transport)
                .build());
        // Outside range — should not be included
        expenseRepository.save(Expense.builder()
                .amount(new BigDecimal("500.00"))
                .expenseDate(LocalDate.of(2025, 8, 1))
                .category(food)
                .build());

        BigDecimal total = expenseRepository.sumAmountByDateRange(start, end);

        assertThat(total.compareTo(new BigDecimal("150.00"))).isZero();
    }
}
