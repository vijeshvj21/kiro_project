package com.project.kiro.report;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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

        // ── 3b. Category Pie Chart ────────────────────────────────────────────
        if (report.getCategoryTotals() != null && !report.getCategoryTotals().isEmpty()) {
            document.add(new Paragraph("Category Breakdown")
                    .setBold()
                    .setFontSize(14)
                    .setMarginTop(16));

            drawPieChart(document, pdfDoc, report.getCategoryTotals());
        }

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

        Table expenseTable = new Table(6);
        expenseTable.setWidth(530);

        expenseTable.addHeaderCell(new Cell().add(new Paragraph("ID").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Category").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
        expenseTable.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()));
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
                        expense.getAmount() != null ? "₹" + expense.getAmount().toPlainString() : "₹0.00")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getTransactionType() != null ? expense.getTransactionType() : "DEBIT")));
                expenseTable.addCell(new Cell().add(new Paragraph(
                        expense.getDescription() != null ? expense.getDescription() : "")));
            }
        }

        document.add(expenseTable);

        // ── Finalize ──────────────────────────────────────────────────────────
        document.close();

        return baos.toByteArray();
    }

    /**
     * Draws a pie chart representing category totals in the PDF document.
     */
    private void drawPieChart(Document document, PdfDocument pdfDoc, List<ReportResponse.CategoryTotal> categoryTotals) {
        try {
            drawPieChartInternal(document, pdfDoc, categoryTotals);
        } catch (Exception e) {
            // If chart drawing fails, just skip it
            document.add(new Paragraph("(Chart rendering failed)")
                    .setFontSize(10).setItalic());
        }
    }

    private void drawPieChartInternal(Document document, PdfDocument pdfDoc, List<ReportResponse.CategoryTotal> categoryTotals) throws Exception {
        // Category colors (RGB)
        int[][] colors = {
            {239, 68, 68},    // Food - Red
            {59, 130, 246},   // Transport - Blue
            {245, 158, 11},   // Shopping - Amber
            {139, 92, 246},   // Entertainment - Purple
            {16, 185, 129},   // Healthcare - Green
            {6, 182, 212},    // Utilities - Cyan
            {236, 72, 153},   // Education - Pink
            {249, 115, 22},   // Investment - Orange
            {107, 114, 128},  // Other - Gray
        };

        // Calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (ReportResponse.CategoryTotal ct : categoryTotals) {
            if (ct.getTotal() != null) total = total.add(ct.getTotal());
        }
        if (total.compareTo(BigDecimal.ZERO) == 0) return;

        float chartSize = 200;
        float centerX = chartSize / 2;
        float centerY = chartSize / 2;
        float radius = 80;

        // Create a form XObject (off-page canvas) to draw the pie
        PdfFormXObject chartXObject = new PdfFormXObject(new com.itextpdf.kernel.geom.Rectangle(chartSize + 200, chartSize));
        PdfCanvas canvas = new PdfCanvas(chartXObject, pdfDoc);

        double startAngle = 0;
        int colorIndex = 0;

        for (ReportResponse.CategoryTotal ct : categoryTotals) {
            if (ct.getTotal() == null || ct.getTotal().compareTo(BigDecimal.ZERO) == 0) continue;

            double fraction = ct.getTotal().doubleValue() / total.doubleValue();
            double sweepAngle = fraction * 360.0;

            int[] color = colors[colorIndex % colors.length];
            canvas.setFillColor(new DeviceRgb(color[0], color[1], color[2]));

            // Draw pie slice using arc + lines to center
            canvas.moveTo(centerX, centerY);
            canvas.arc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                    (float) startAngle, (float) sweepAngle);
            canvas.lineTo(centerX, centerY);
            canvas.closePathFillStroke();

            // Draw legend entry on the right side
            float legendX = chartSize + 10;
            float legendY = chartSize - 20 - (colorIndex * 18);
            canvas.setFillColor(new DeviceRgb(color[0], color[1], color[2]));
            canvas.rectangle(legendX, legendY - 4, 10, 10);
            canvas.fill();

            // Legend text
            canvas.beginText();
            canvas.setFillColor(new DeviceRgb(0, 0, 0));
            canvas.setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(), 9);
            canvas.moveText(legendX + 14, legendY - 2);
            String label = ct.getCategoryName() + " - ₹" + ct.getTotal().toPlainString()
                    + " (" + String.format("%.0f", fraction * 100) + "%)";
            canvas.showText(label);
            canvas.endText();

            startAngle += sweepAngle;
            colorIndex++;
        }

        // Add the chart image to the document
        Image chartImage = new Image(chartXObject);
        chartImage.setHorizontalAlignment(HorizontalAlignment.LEFT);
        chartImage.setMarginTop(8);
        chartImage.setMarginBottom(8);
        document.add(chartImage);
    }
}
