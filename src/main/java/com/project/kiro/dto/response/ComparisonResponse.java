package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {

    private MonthSummary specifiedMonth;
    private MonthSummary precedingMonth;
    private BigDecimal absoluteDifference;
    private BigDecimal percentageChange;
    private boolean percentageChangeAvailable;
    private List<CategoryComparison> categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthSummary {
        private int year;
        private int month;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryComparison {
        private String categoryName;
        private BigDecimal specifiedMonthTotal;
        private BigDecimal precedingMonthTotal;
    }
}
