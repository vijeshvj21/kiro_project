package com.project.kiro.report;

import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Generates a CSV report from a {@link ReportResponse}.
 * <p>
 * The output contains:
 * <ol>
 *   <li>A header row: {@code ID,Date,Category,Amount,Description}</li>
 *   <li>One data row per expense (already sorted date-descending by ReportService)</li>
 *   <li>A summary section: overall total, per-category totals, weekly subtotals,
 *       monthly subtotals</li>
 * </ol>
 *
 * Satisfies Requirements 11.4, 11.5.
 */
@Component
public class CsvReportGenerator {

    /**
     * Generate a CSV report as a UTF-8 encoded byte array.
     *
     * @param report the fully-populated report response
     * @return the CSV content as {@code byte[]}
     */
    public byte[] generate(ReportResponse report) {
        StringWriter sw = new StringWriter();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Date", "Category", "Amount", "Type", "Description")
                .build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {

            // One row per expense (already sorted date-descending by ReportService)
            for (ExpenseResponse expense : report.getExpenses()) {
                printer.printRecord(
                        expense.getId(),
                        expense.getExpenseDate(),
                        expense.getCategoryName(),
                        expense.getAmount(),
                        expense.getTransactionType() != null ? expense.getTransactionType() : "DEBIT",
                        expense.getDescription()
                );
            }

            // Empty separator row
            printer.println();

            // Overall total
            printer.printRecord("TOTAL", "", "", "", report.getTotal());

            // Per-category totals
            if (report.getCategoryTotals() != null) {
                for (ReportResponse.CategoryTotal ct : report.getCategoryTotals()) {
                    printer.printRecord("CATEGORY_TOTAL", ct.getCategoryName(), "", "", ct.getTotal());
                }
            }

            // Per-week subtotals
            if (report.getWeeklySubtotals() != null) {
                for (ReportResponse.WeeklySubtotal ws : report.getWeeklySubtotals()) {
                    String period = ws.getYear() + "-W" + String.format("%02d", ws.getWeek());
                    printer.printRecord("WEEKLY_TOTAL", "", period, "", ws.getTotal());
                }
            }

            // Per-month subtotals
            if (report.getMonthlySubtotals() != null) {
                for (ReportResponse.MonthlySubtotal ms : report.getMonthlySubtotals()) {
                    String period = ms.getYear() + "-" + String.format("%02d", ms.getMonth());
                    printer.printRecord("MONTHLY_TOTAL", "", period, "", ms.getTotal());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }
}
