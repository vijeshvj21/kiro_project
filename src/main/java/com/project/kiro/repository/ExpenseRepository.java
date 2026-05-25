package com.project.kiro.repository;

import com.project.kiro.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Return all expenses whose date falls within [start, end], sorted by date descending.
     * Reused for date-range filtering, weekly view, and monthly view.
     * Satisfies Requirements 3.1, 4.1.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return list of matching expenses ordered by expenseDate descending
     */
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate start, LocalDate end);

    /**
     * Return the most recent 1000 expenses sorted by date descending.
     * Satisfies Requirement 2.7.
     *
     * @return up to 1000 expenses ordered by expenseDate descending
     */
    List<Expense> findTop1000ByOrderByExpenseDateDesc();

    /**
     * Return the sum of all expense amounts whose date falls within [start, end].
     * Returns 0 (via COALESCE) when no expenses exist in the range.
     * Satisfies Requirements 3.4, 4.4, 9.1, 10.1.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return total amount for the date range, never null
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumAmountByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Return per-category totals for expenses whose date falls within [start, end],
     * ordered by category name ascending.
     * Each element is an Object[] where [0] = category name (String) and [1] = sum (BigDecimal).
     * Satisfies Requirement 4.5.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return list of [categoryName, total] pairs ordered by category name
     */
    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category.name ORDER BY e.category.name")
    List<Object[]> findCategoryBreakdown(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Return per-category totals for expenses whose date falls within [start, end]
     * without a fixed ordering (used by ComparisonService which builds its own map).
     * Each element is an Object[] where [0] = category name (String) and [1] = sum (BigDecimal).
     * Satisfies Requirements 9.5, 10.5.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return list of [categoryName, total] pairs
     */
    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category.name")
    List<Object[]> sumByCategoryAndDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
