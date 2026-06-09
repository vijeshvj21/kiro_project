package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyExpenseResponse {

    private int year;
    private int week;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal total;
    private int entryCount;
    private List<ExpenseResponse> expenses;
}
