package com.project.kiro.report;

import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CsvReportGenerator}.
 * No Spring context, no Mockito — CsvReportGenerator has no dependencies.
 *
 * Validates Requirements 11.4, 11.5.
 */
class CsvReportGeneratorTest {

    private CsvReportGenerator generator;
    private ReportResponse report;

    @BeforeEach
    void setUp() {
        generator = new CsvReportGenerator();

        List<ExpenseResponse> expenses = Arrays.asList(
                ExpenseResponse.builder()
                        .id(1L)
                        .amount(new BigDecimal("50.00"))
                        .expenseDate(LocalDate.of(2024, 1, 10))
                        .categoryId(1L)
                        .categoryName("Food")
                        .description("Lunch")
                        .build(),
                ExpenseResponse.builder()
                        .id(2L)
                        .amount(new BigDecimal("120.50"))
                        .expenseDate(LocalDate.of(2024, 1, 15))
                        .categoryId(2L)
                        .categoryName("Transport")
                        .description("Bus pass")
                        .build(),
                ExpenseResponse.builder()
                        .id(3L)
                        .amount(new BigDecimal("30.00"))
                        .expenseDate(LocalDate.of(2024, 1, 20))
                        .categoryId(1L)
                        .categoryName("Food")
                        .description("Dinner")
                        .build()
        );

        List<ReportResponse.CategoryTotal> categoryTotals = Arrays.asList(
                ReportResponse.CategoryTotal.builder()
                        .categoryName("Food")
                        .total(new BigDecimal("80.00"))
                        .build(),
                ReportResponse.CategoryTotal.builder()
                        .categoryName("Transport")
                        .total(new BigDecimal("120.50"))
                        .build()
        );

        List<ReportResponse.WeeklySubtotal> weeklySubtotals = Collections.singletonList(
                ReportResponse.WeeklySubtotal.builder()
                        .year(2024)
                        .week(3)
                        .total(new BigDecimal("200.50"))
                        .build()
        );

        List<ReportResponse.MonthlySubtotal> monthlySubtotals = Collections.singletonList(
                ReportResponse.MonthlySubtotal.builder()
                        .year(2024)
                        .month(1)
                        .total(new BigDecimal("200.50"))
                        .build()
        );

        report = ReportResponse.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .total(new BigDecimal("200.50"))
                .expenses(expenses)
                .categoryTotals(categoryTotals)
                .weeklySubtotals(weeklySubtotals)
                .monthlySubtotals(monthlySubtotals)
                .build();
    }

    /** Helper: decode bytes to String and split by newline, normalising CRLF first. */
    private String[] toLines(byte[] csv) {
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        // Normalise CRLF -> LF before splitting
        return content.replace("\r\n", "\n").split("\n");
    }

    // ── Requirement 11.4 / 11.5 ───────────────────────────────────────────────

    @Test
    void headerRowContainsAllFiveColumnNames() {
        byte[] csv = generator.generate(report);
        String[] lines = toLines(csv);

        String header = lines[0];
        assertTrue(header.contains("ID"),          "Header must contain 'ID'");
        assertTrue(header.contains("Date"),        "Header must contain 'Date'");
        assertTrue(header.contains("Category"),    "Header must contain 'Category'");
        assertTrue(header.contains("Amount"),      "Header must contain 'Amount'");
        assertTrue(header.contains("Description"), "Header must contain 'Description'");
    }

    @Test
    void threeDataRowsFollowTheHeader() {
        byte[] csv = generator.generate(report);
        String[] lines = toLines(csv);

        // lines[0] = header; lines[1..3] = data rows; lines[4] = empty separator
        assertEquals(3, countNonEmptyDataRows(lines),
                "There should be exactly 3 data rows after the header");
    }

    @Test
    void summarySectionIsPresentAfterEmptySeparatorRow() {
        byte[] csv = generator.generate(report);
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(content.contains("TOTAL"),
                "CSV should contain a summary row with 'TOTAL'");
    }

    @Test
    void eachDataRowContainsCorrectExpenseValues() {
        byte[] csv = generator.generate(report);
        String[] lines = toLines(csv);

        // lines[1] -> expense id=1
        String row1 = lines[1];
        assertTrue(row1.contains("1"),          "Row 1 should contain id=1");
        assertTrue(row1.contains("2024-01-10"), "Row 1 should contain expenseDate");
        assertTrue(row1.contains("Food"),       "Row 1 should contain category 'Food'");
        assertTrue(row1.contains("50.00"),      "Row 1 should contain amount 50.00");
        assertTrue(row1.contains("Lunch"),      "Row 1 should contain description 'Lunch'");

        // lines[2] -> expense id=2
        String row2 = lines[2];
        assertTrue(row2.contains("2"),          "Row 2 should contain id=2");
        assertTrue(row2.contains("2024-01-15"), "Row 2 should contain expenseDate");
        assertTrue(row2.contains("Transport"),  "Row 2 should contain category 'Transport'");
        assertTrue(row2.contains("120.50"),     "Row 2 should contain amount 120.50");
        assertTrue(row2.contains("Bus pass"),   "Row 2 should contain description 'Bus pass'");

        // lines[3] -> expense id=3
        String row3 = lines[3];
        assertTrue(row3.contains("3"),          "Row 3 should contain id=3");
        assertTrue(row3.contains("2024-01-20"), "Row 3 should contain expenseDate");
        assertTrue(row3.contains("Food"),       "Row 3 should contain category 'Food'");
        assertTrue(row3.contains("30.00"),      "Row 3 should contain amount 30.00");
        assertTrue(row3.contains("Dinner"),     "Row 3 should contain description 'Dinner'");
    }

    @Test
    void categoryTotalsArePresentInSummarySection() {
        byte[] csv = generator.generate(report);
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(content.contains("CATEGORY_TOTAL"),
                "Summary should contain 'CATEGORY_TOTAL' rows");
        assertTrue(content.contains("Food"),      "Summary should list Food category total");
        assertTrue(content.contains("Transport"), "Summary should list Transport category total");
    }

    @Test
    void weeklyAndMonthlyTotalsArePresentInSummarySection() {
        byte[] csv = generator.generate(report);
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(content.contains("WEEKLY_TOTAL"),  "Summary should contain 'WEEKLY_TOTAL'");
        assertTrue(content.contains("MONTHLY_TOTAL"), "Summary should contain 'MONTHLY_TOTAL'");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Count non-empty lines between the header (index 0) and the first empty
     * separator line (which marks the start of the summary section).
     */
    private int countNonEmptyDataRows(String[] lines) {
        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                break;
            }
            count++;
        }
        return count;
    }
}
