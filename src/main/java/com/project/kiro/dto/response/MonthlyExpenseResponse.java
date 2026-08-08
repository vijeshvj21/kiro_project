package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpenseResponse {

    private int year;
    private int month;
    private BigDecimal total;
    private int entryCount;
    private List<CategoryTotal> categoryBreakdown;
    private List<ExpenseResponse> expenses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTotal {
        private String categoryName;
        private BigDecimal total;
    }
}
