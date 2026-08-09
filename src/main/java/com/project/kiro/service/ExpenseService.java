package com.project.kiro.service;

import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.MonthlyExpenseResponse;
import com.project.kiro.dto.response.WeeklyExpenseResponse;
import com.project.kiro.exception.ResourceNotFoundException;
import com.project.kiro.model.Category;
import com.project.kiro.model.Expense;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing expenses.
 * Satisfies Requirements 1.1–1.9, 2.1–2.7.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Create a new expense.
     * Validates that the referenced category exists, builds the entity, persists it,
     * and returns the mapped response.
     * Satisfies Requirements 1.1–1.9.
     *
     * @param request the expense creation request
     * @return the created expense as an ExpenseResponse
     * @throws ResourceNotFoundException if the referenced category does not exist
     */
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .category(category)
                .description(request.getDescription())
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    /**
     * Retrieve a single expense by its ID.
     * Satisfies Requirement 2.1.
     *
     * @param id the ID of the expense to retrieve
     * @return the expense as an ExpenseResponse
     * @throws ResourceNotFoundException if no expense with the given ID exists
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        return toResponse(expense);
    }

    /**
     * Update an existing expense.
     * Validates that both the expense and the referenced category exist, updates all
     * fields, persists, and returns the mapped response.
     * Satisfies Requirements 2.1–2.3.
     *
     * @param id      the ID of the expense to update
     * @param request the update request containing new field values
     * @return the updated expense as an ExpenseResponse
     * @throws ResourceNotFoundException if the expense or the referenced category does not exist
     */
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCategory(category);
        expense.setDescription(request.getDescription());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    /**
     * Delete an expense by its ID.
     * Validates existence before deleting.
     * Satisfies Requirements 2.4–2.6.
     *
     * @param id the ID of the expense to delete
     * @throws ResourceNotFoundException if no expense with the given ID exists
     */
    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        expenseRepository.deleteById(id);
    }

    /**
     * Retrieve all expenses sorted by expense date descending, capped at 1000 records.
     * Satisfies Requirement 2.7.
     *
     * @return list of up to 1000 expenses ordered by expenseDate descending
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findTop1000ByOrderByExpenseDateDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all expenses for a given ISO week, along with the total amount.
     * Satisfies Requirements 3.1–3.4.
     *
     * @param year the ISO week-based year (1900–2100 inclusive)
     * @param week the ISO week number (1–53 inclusive, must exist in the given year)
     * @return a WeeklyExpenseResponse containing the week range, expenses, and total
     * @throws IllegalArgumentException if year or week is out of range or the week does not exist in the year
     */
    @Transactional(readOnly = true)
    public WeeklyExpenseResponse getWeeklyExpenses(int year, int week) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }
        if (week < 1 || week > 53) {
            throw new IllegalArgumentException("Week number must be between 1 and 53");
        }

        // Determine the maximum ISO week number for the given year
        int maxWeek = (int) WeekFields.ISO.weekOfWeekBasedYear()
                .rangeRefinedBy(LocalDate.of(year, 6, 1))
                .getMaximum();
        if (week > maxWeek) {
            throw new IllegalArgumentException("Week number must be between 1 and 53");
        }

        // Compute Monday–Sunday range for the requested ISO week
        LocalDate weekStart = LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Expense> expenses = expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDesc(weekStart, weekEnd);
        BigDecimal total = expenseRepository.sumAmountByDateRange(weekStart, weekEnd);

        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return WeeklyExpenseResponse.builder()
                .year(year)
                .week(week)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .total(total)
                .entryCount(expenseResponses.size())
                .expenses(expenseResponses)
                .build();
    }

    /**
     * Retrieve a monthly summary of expenses for the given year and month.
     * Validates year (1900–2100) and month (1–12), queries all expenses in the
     * calendar month, computes the total and per-category breakdown, and returns
     * a {@link MonthlyExpenseResponse}.
     * Satisfies Requirements 4.1–4.5.
     *
     * @param year  the calendar year (1900–2100 inclusive)
     * @param month the calendar month (1–12 inclusive)
     * @return a monthly expense summary
     * @throws IllegalArgumentException if year or month is out of range
     */
    @Transactional(readOnly = true)
    public MonthlyExpenseResponse getMonthlyExpenses(int year, int month) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
        BigDecimal total = expenseRepository.sumDebitAmountByDateRange(start, end);

        List<MonthlyExpenseResponse.CategoryTotal> categoryBreakdown =
                expenseRepository.findDebitCategoryBreakdown(start, end)
                        .stream()
                        .map(row -> MonthlyExpenseResponse.CategoryTotal.builder()
                                .categoryName((String) row[0])
                                .total((BigDecimal) row[1])
                                .build())
                        .collect(Collectors.toList());

        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return MonthlyExpenseResponse.builder()
                .year(year)
                .month(month)
                .total(total)
                .entryCount(expenseResponses.size())
                .categoryBreakdown(categoryBreakdown)
                .expenses(expenseResponses)
                .build();
    }

    /**
     * Map an Expense entity to an ExpenseResponse DTO.
     *
     * @param expense the entity to map
     * @return the mapped response
     */
    private ExpenseResponse toResponse(Expense expense) {
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
