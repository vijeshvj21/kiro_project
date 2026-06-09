package com.project.kiro.report;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Generates a PDF report from a {@link ReportResponse} using iText 7 Community.
 * <p>
 * The generated document contains:
 * <ol>
 *   <li>Title heading: "Expense Report"</li>
 *   <li>Date range subtitle: "Period: {startDate} to {endDate}"</li>
 *   <li>Summary table: overall total + per-category totals</li>
 *   <li>Weekly subtotals table (if non-empty)</li>
 *   <li>Monthly subtotals table (if non-empty)</li>
 *   <li>Full expense list table sorted by date descending (as provided by ReportService)</li>
 * </ol>
 *
 * Satisfies Requirements 11.4, 11.6.
 */
@Component
public class PdfReportGenerator {

    /**
     * Generate a PDF report as a byte array suitable for streaming as {@code application/pdf}.
     *
     * @param report the fully-populated report response
     * @return the PDF content as {@code byte[]}
     */
    public byte[] generate(ReportResponse report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // ── 1. Title ──────────────────────────────────────────────────────────
        document.add(new Paragraph("Expense Report")
                .setBold()
                .setFontSize(20));

        // ── 2. Date range header ──────────────────────────────────────────────
        String dateRange = "Period: " + report.getStartDate() + " to " + report.getEndDate();
        document.add(new Paragraph(dateRange)
                .setFontSize(12)
                .setMarginBottom(12));

        // ── 3. Summary table (Total + per-category totals) ────────────────────
        document.add(new Paragraph("Summary")
                .setBold()
                .setFontSize(14)
                .setMarginTop(8));

        Table summaryTable = new Table(2);
        summaryTable.setWidth(300);

        // Header row
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Label").setBold()));
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));

        // Overall total row
        summaryTable.addCell(new Cell().add(new Paragraph("Total")));
        summaryTable.addCell(new Cell().add(new Paragraph(
                report.getTotal() != null ? report.getTotal().toPlainString() : "0.00")));

        // Per-category total rows
        if (report.getCategoryTotals() != null) {
            for (ReportResponse.CategoryTotal ct : report.getCategoryTotals()) {
                summaryTable.addCell(new Cell().add(new Paragraph(ct.getCategoryName())));
                summaryTable.addCell(new Cell().add(new Paragraph(
                        ct.getTotal() != null ? ct.getTotal().toPlainString() : "0.00")));
            }
        }

        document.add(summaryTable);

        // ── 4. Weekly subtotals table (if non-empty) ──────────────────────────
        List<ReportResponse.WeeklySubtotal> weeklySubtotals = report.getWeeklySubtotals();
        if (weeklySubtotals != null && !weeklySubtotals.isEmpty()) {
            document.add(new Paragraph("Weekly Subtotals")
                    .setBold()
                    .setFontSize(14)
                    .setMarginTop(12));

            Table weeklyTable = new Table(2);
            weeklyTable.setWidth(300);

            weeklyTable.addHeaderCell(new Cell().add(new Paragraph("Year-Week").setBold()));
            weeklyTable.addHeaderCell(new Cell().add(new Paragraph("Total").setBold()));

            for (ReportResponse.WeeklySubtotal ws : weeklySubtotals) {
                String yearWeek = ws.getYear() + "-W" + String.format("%02d", ws.getWeek());
                weeklyTable.addCell(new Cell().add(new Paragraph(yearWeek)));
                weeklyTable.addCell(new Cell().add(new Paragraph(
                        ws.getTotal() != null ? ws.getTotal().toPlainString() : "0.00")));
            }

            document.add(weeklyTable);
        }

        // ── 5. Monthly subtotals table (if non-empty) ─────────────────────────
        List<ReportResponse.MonthlySubtotal> monthlySubtotals = report.getMonthlySubtotals();
        if (monthlySubtotals != null && !monthlySubtotals.isEmpty()) {
            document.add(new Paragraph("Monthly Subtotals")
                    .setBold()
                    .setFontSize(14)
                    .setMarginTop(12));

            Table monthlyTable = new Table(2);
            monthlyTable.setWidth(300);

            monthlyTable.addHeaderCell(new Cell().add(new Paragraph("Year-Month").setBold()));
            monthlyTable.addHeaderCell(new Cell().add(new Paragraph("Total").setBold()));

            for (ReportResponse.MonthlySubtotal ms : monthlySubtotals) {
                String yearMonth = ms.getYear() + "-" + String.format("%02d", ms.getMonth());
                monthlyTable.addCell(new Cell().add(new Paragraph(yearMonth)));
                monthlyTable.addCell(new Cell().add(new Paragraph(
                        ms.getTotal() != null ? ms.getTotal().toPlainString() : "0.00")));
            }

            document.add(monthlyTable);
        }

        // ── 6. Expense list table (sorted date-descending by ReportService) ───
        document.add(new Paragraph("Expenses")
                .setBold()
                .setFontSize(14)
                .setMarginTop(12));

        Table expenseTable = new Table(5);
        expenseTable.setWidth(500);

        expenseTable.addHeaderCell(new Cell().add(new Paragraph("ID").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Category").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));

        List<ExpenseResponse> expenses = report.getExpenses();
        if (expenses != null) {
            for (ExpenseResponse expense : expenses) {
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getId() != null ? expense.getId().toString() : "")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getCategoryName() != null ? expense.getCategoryName() : "")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getAmount() != null ? expense.getAmount().toPlainString() : "0.00")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getDescription() != null ? expense.getDescription() : "")));
            }
        }

        document.add(expenseTable);

        // ── Finalize ──────────────────────────────────────────────────────────
        document.close();

        return baos.toByteArray();
    }
}
