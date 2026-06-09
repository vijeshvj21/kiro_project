package com.project.kiro.controller;

import com.project.kiro.dto.request.ReportRequest;
import com.project.kiro.dto.response.ReportResponse;
import com.project.kiro.report.CsvReportGenerator;
import com.project.kiro.report.PdfReportGenerator;
import com.project.kiro.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for expense report generation.
 * <p>
 * Exposes:
 * <ul>
 *   <li>{@code POST /api/v1/reports/csv} — generates a CSV report</li>
 *   <li>{@code POST /api/v1/reports/pdf} — generates a PDF report</li>
 *   <li>{@code POST /api/v1/reports/{format}} — catch-all that returns 400 for unsupported formats</li>
 * </ul>
 *
 * Satisfies Requirements 11.1–11.8.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final CsvReportGenerator csvGenerator;
    private final PdfReportGenerator pdfGenerator;

    public ReportController(ReportService reportService,
                            CsvReportGenerator csvGenerator,
                            PdfReportGenerator pdfGenerator) {
        this.reportService = reportService;
        this.csvGenerator = csvGenerator;
        this.pdfGenerator = pdfGenerator;
    }

    /**
     * Generate and download a CSV expense report.
     *
     * @param request the report parameters (date range or predefined period)
     * @return a {@code text/csv} attachment with filename {@code report.csv}
     */
    @PostMapping("/csv")
    public ResponseEntity<byte[]> generateCsvReport(@RequestBody ReportRequest request) {
        ReportResponse reportData = reportService.generateReport(request);
        byte[] csvBytes = csvGenerator.generate(reportData);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"")
                .body(csvBytes);
    }

    /**
     * Generate and download a PDF expense report.
     *
     * @param request the report parameters (date range or predefined period)
     * @return an {@code application/pdf} attachment with filename {@code report.pdf}
     */
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(@RequestBody ReportRequest request) {
        ReportResponse reportData = reportService.generateReport(request);
        byte[] pdfBytes = pdfGenerator.generate(reportData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\"")
                .body(pdfBytes);
    }

    /**
     * Catch-all for unsupported format paths under {@code /api/v1/reports/*}.
     * <p>
     * Throws {@link IllegalArgumentException} which is handled by
     * {@link com.project.kiro.exception.GlobalExceptionHandler} as HTTP 400.
     *
     * @param format the unsupported format path segment
     */
    @PostMapping("/{format}")
    public ResponseEntity<byte[]> unsupportedFormat(@PathVariable String format) {
        throw new IllegalArgumentException(
                "Unsupported report format. Supported formats are: CSV, PDF");
    }
}
