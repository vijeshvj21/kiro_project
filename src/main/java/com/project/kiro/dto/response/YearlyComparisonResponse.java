package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyComparisonResponse {

    private YearSummary specifiedYear;
    private YearSummary precedingYear;
    private BigDecimal absoluteDifference;
    private BigDecimal percentageChange;
    private boolean percentageChangeAvailable;
    private List<MonthlyComparison> monthlyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearSummary {
        private int year;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private int month;
        private BigDecimal specifiedYearTotal;
        private BigDecimal precedingYearTotal;
    }
}
