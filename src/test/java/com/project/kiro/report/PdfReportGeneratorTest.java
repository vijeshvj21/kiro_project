package com.project.kiro.report;

import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PdfReportGenerator}.
 * No Spring context, no Mockito — PdfReportGenerator has no dependencies.
 *
 * Validates Requirements 11.4, 11.6.
 */
class PdfReportGeneratorTest {

    private PdfReportGenerator generator;
    private ReportResponse report;

    @BeforeEach
    void setUp() {
        generator = new PdfReportGenerator();

        report = ReportResponse.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .total(new BigDecimal("200.50"))
                .expenses(Arrays.asList(
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
                ))
                .categoryTotals(Arrays.asList(
                        ReportResponse.CategoryTotal.builder()
                                .categoryName("Food")
                                .total(new BigDecimal("80.00"))
                                .build(),
                        ReportResponse.CategoryTotal.builder()
                                .categoryName("Transport")
                                .total(new BigDecimal("120.50"))
                                .build()
                ))
                .weeklySubtotals(Collections.singletonList(
                        ReportResponse.WeeklySubtotal.builder()
                                .year(2024)
                                .week(3)
                                .total(new BigDecimal("200.50"))
                                .build()
                ))
                .monthlySubtotals(Collections.singletonList(
                        ReportResponse.MonthlySubtotal.builder()
                                .year(2024)
                                .month(1)
                                .total(new BigDecimal("200.50"))
                                .build()
                ))
                .build();
    }

    // ── Requirement 11.4 / 11.6 ───────────────────────────────────────────────

    @Test
    void generateReturnsNonNull() {
        byte[] result = generator.generate(report);
        assertNotNull(result, "generate() must not return null");
    }

    @Test
    void generateReturnsNonEmptyByteArray() {
        byte[] result = generator.generate(report);
        assertTrue(result.length > 0, "generate() must return a non-empty byte array");
    }

    @Test
    void generateReturnsValidPdfMagicBytes() {
        byte[] result = generator.generate(report);

        // Every PDF begins with the ASCII bytes for "%PDF"
        assertTrue(result.length >= 4, "PDF must be at least 4 bytes long");
        assertEquals('%', (char) result[0], "Byte 0 must be '%'");
        assertEquals('P', (char) result[1], "Byte 1 must be 'P'");
        assertEquals('D', (char) result[2], "Byte 2 must be 'D'");
        assertEquals('F', (char) result[3], "Byte 3 must be 'F'");
    }

    @Test
    void generateDoesNotThrowForEmptyExpenseList() {
        ReportResponse emptyReport = ReportResponse.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .total(BigDecimal.ZERO)
                .expenses(Collections.emptyList())
                .categoryTotals(Collections.emptyList())
                .weeklySubtotals(Collections.emptyList())
                .monthlySubtotals(Collections.emptyList())
                .build();

        assertDoesNotThrow(() -> generator.generate(emptyReport),
                "generate() must not throw when the expense list is empty");
    }

    @Test
    void generateDoesNotThrowForNullExpenseList() {
        ReportResponse nullExpensesReport = ReportResponse.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .total(BigDecimal.ZERO)
                .expenses(null)
                .categoryTotals(null)
                .weeklySubtotals(null)
                .monthlySubtotals(null)
                .build();

        assertDoesNotThrow(() -> generator.generate(nullExpensesReport),
                "generate() must not throw when expense list is null");
    }
}
