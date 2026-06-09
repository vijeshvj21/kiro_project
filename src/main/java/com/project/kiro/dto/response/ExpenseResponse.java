package com.project.kiro.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private Long categoryId;
    private String categoryName;
    private String description;
}
