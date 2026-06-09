package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal total;
    private List<CategoryTotal> categoryTotals;
    private List<WeeklySubtotal> weeklySubtotals;
    private List<MonthlySubtotal> monthlySubtotals;
    private List<ExpenseResponse> expenses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTotal {
        private String categoryName;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklySubtotal {
        private int year;
        private int week;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySubtotal {
        private int year;
        private int month;
        private BigDecimal total;
    }
}
