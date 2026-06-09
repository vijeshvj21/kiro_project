package com.project.kiro.service;

import com.project.kiro.dto.request.ReportRequest;
import com.project.kiro.dto.response.ReportResponse;
import com.project.kiro.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReportService} predefined period resolution.
 * Validates Requirement 11.7.
 */
@ExtendWith(MockitoExtension.class)
class ReportServicePredefinedPeriodTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportService reportService;

    // -------------------------------------------------------------------------
    // Shared mock setup helper
    // -------------------------------------------------------------------------

    /**
     * Stubs all repository methods called by generateReport so tests can focus
     * solely on start/end date resolution.
     */
    private void stubRepositoryEmpty() {
        when(expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.findCategoryBreakdown(any(), any()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.sumAmountByDateRange(any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // CURRENT_WEEK
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — CURRENT_WEEK resolves to Monday..Sunday of the current ISO week.
     */
    @Test
    void predefinedPeriod_currentWeek_resolvesToIsoWeekMondayToSunday() {
        stubRepositoryEmpty();

        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("CURRENT_WEEK")
                .build();

        ReportResponse report = reportService.generateReport(request);

        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.with(DayOfWeek.MONDAY);
        LocalDate expectedEnd   = today.with(DayOfWeek.SUNDAY);

        assertThat(report.getStartDate()).isEqualTo(expectedStart);
        assertThat(report.getEndDate()).isEqualTo(expectedEnd);
    }

    // -------------------------------------------------------------------------
    // CURRENT_MONTH
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — CURRENT_MONTH resolves to the first and last day of the current month.
     */
    @Test
    void predefinedPeriod_currentMonth_resolvesToFirstAndLastDayOfCurrentMonth() {
        stubRepositoryEmpty();

        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("CURRENT_MONTH")
                .build();

        ReportResponse report = reportService.generateReport(request);

        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.withDayOfMonth(1);
        LocalDate expectedEnd   = today.withDayOfMonth(today.lengthOfMonth());

        assertThat(report.getStartDate()).isEqualTo(expectedStart);
        assertThat(report.getEndDate()).isEqualTo(expectedEnd);
    }

    // -------------------------------------------------------------------------
    // CURRENT_YEAR
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — CURRENT_YEAR resolves to Jan 1 .. Dec 31 of the current year.
     */
    @Test
    void predefinedPeriod_currentYear_resolvesToJan1ToDoc31OfCurrentYear() {
        stubRepositoryEmpty();

        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("CURRENT_YEAR")
                .build();

        ReportResponse report = reportService.generateReport(request);

        int currentYear = LocalDate.now().getYear();
        LocalDate expectedStart = LocalDate.of(currentYear, 1, 1);
        LocalDate expectedEnd   = LocalDate.of(currentYear, 12, 31);

        assertThat(report.getStartDate()).isEqualTo(expectedStart);
        assertThat(report.getEndDate()).isEqualTo(expectedEnd);
    }

    // -------------------------------------------------------------------------
    // LAST_MONTH
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — LAST_MONTH resolves to the first and last day of the previous month.
     */
    @Test
    void predefinedPeriod_lastMonth_resolvesToFirstAndLastDayOfPreviousMonth() {
        stubRepositoryEmpty();

        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("LAST_MONTH")
                .build();

        ReportResponse report = reportService.generateReport(request);

        LocalDate firstOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastOfLastMonth  = firstOfLastMonth.withDayOfMonth(firstOfLastMonth.lengthOfMonth());

        assertThat(report.getStartDate()).isEqualTo(firstOfLastMonth);
        assertThat(report.getEndDate()).isEqualTo(lastOfLastMonth);
    }

    // -------------------------------------------------------------------------
    // LAST_YEAR
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — LAST_YEAR resolves to Jan 1 .. Dec 31 of the previous year.
     */
    @Test
    void predefinedPeriod_lastYear_resolvesToJan1ToDec31OfPreviousYear() {
        stubRepositoryEmpty();

        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("LAST_YEAR")
                .build();

        ReportResponse report = reportService.generateReport(request);

        int lastYear = LocalDate.now().getYear() - 1;
        LocalDate expectedStart = LocalDate.of(lastYear, 1, 1);
        LocalDate expectedEnd   = LocalDate.of(lastYear, 12, 31);

        assertThat(report.getStartDate()).isEqualTo(expectedStart);
        assertThat(report.getEndDate()).isEqualTo(expectedEnd);
    }

    // -------------------------------------------------------------------------
    // Unsupported period
    // -------------------------------------------------------------------------

    /**
     * Requirement 11.7 — an unknown predefined period string throws IllegalArgumentException.
     */
    @Test
    void predefinedPeriod_unknownPeriod_throwsIllegalArgumentException() {
        ReportRequest request = ReportRequest.builder()
                .predefinedPeriod("INVALID_PERIOD")
                .build();

        assertThatThrownBy(() -> reportService.generateReport(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_PERIOD");
    }
}
