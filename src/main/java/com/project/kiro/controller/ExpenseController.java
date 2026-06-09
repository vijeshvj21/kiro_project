package com.project.kiro.controller;

import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.ComparisonResponse;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.MonthlyExpenseResponse;
import com.project.kiro.dto.response.WeeklyExpenseResponse;
import com.project.kiro.dto.response.YearlyComparisonResponse;
import com.project.kiro.service.ComparisonService;
import com.project.kiro.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing expenses.
 * Satisfies Requirements 1.1–1.9, 2.1–2.7, 9.1–9.7, 10.1–10.5.
 */
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ComparisonService comparisonService;

    public ExpenseController(ExpenseService expenseService, ComparisonService comparisonService) {
        this.expenseService = expenseService;
        this.comparisonService = comparisonService;
    }

    /**
     * Create a new expense.
     * Satisfies Requirements 1.1–1.9.
     *
     * @param request the expense creation request (validated)
     * @return 201 Created with the created expense
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieve all expenses sorted by expense date descending.
     * Satisfies Requirement 2.7.
     *
     * @return 200 OK with list of all expenses
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
        List<ExpenseResponse> expenses = expenseService.getAllExpenses();
        return ResponseEntity.ok(expenses);
    }

    /**
     * Retrieve a single expense by its ID.
     * Satisfies Requirement 2.1.
     *
     * @param id the ID of the expense to retrieve
     * @return 200 OK with the expense
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        ExpenseResponse expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(expense);
    }

    /**
     * Update an existing expense.
     * Satisfies Requirements 2.1–2.3.
     *
     * @param id      the ID of the expense to update
     * @param request the update request containing new field values (validated)
     * @return 200 OK with the updated expense
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse updated = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an expense by its ID.
     * Satisfies Requirements 2.4–2.6.
     *
     * @param id the ID of the expense to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieve all expenses for a given ISO week.
     * Satisfies Requirements 3.1–3.4.
     *
     * @param year the ISO week-based year
     * @param week the ISO week number (1–53)
     * @return 200 OK with weekly expense summary
     */
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyExpenseResponse> getWeeklyExpenses(
            @RequestParam int year,
            @RequestParam int week) {
        WeeklyExpenseResponse response = expenseService.getWeeklyExpenses(year, week);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve a monthly summary of expenses for the given year and month.
     * Satisfies Requirements 4.1–4.5.
     *
     * @param year  the calendar year
     * @param month the calendar month (1–12)
     * @return 200 OK with monthly expense summary
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyExpenseResponse> getMonthlyExpenses(
            @RequestParam int year,
            @RequestParam int month) {
        MonthlyExpenseResponse response = expenseService.getMonthlyExpenses(year, month);
        return ResponseEntity.ok(response);
    }

    /**
     * Compare the specified month against the immediately preceding calendar month.
     * Satisfies Requirements 9.1–9.7.
     *
     * @param year  the year of the specified month
     * @param month the month number (1–12) of the specified month
     * @return 200 OK with month-over-month comparison data
     */
    @GetMapping("/comparison/monthly")
    public ResponseEntity<ComparisonResponse> getMonthlyComparison(
            @RequestParam int year,
            @RequestParam int month) {
        ComparisonResponse response = comparisonService.compareMonths(year, month);
        return ResponseEntity.ok(response);
    }

    /**
     * Compare the specified year against the immediately preceding calendar year.
     * Satisfies Requirements 10.1–10.5.
     *
     * @param year the specified year
     * @return 200 OK with year-over-year comparison data
     */
    @GetMapping("/comparison/yearly")
    public ResponseEntity<YearlyComparisonResponse> getYearlyComparison(
            @RequestParam int year) {
        YearlyComparisonResponse response = comparisonService.compareYears(year);
        return ResponseEntity.ok(response);
    }
}
