package com.project.kiro.dto.request;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * Predefined period shortcut. Accepted values:
     * CURRENT_WEEK, CURRENT_MONTH, CURRENT_YEAR, LAST_MONTH, LAST_YEAR.
     * When set, startDate and endDate are ignored.
     */
    private String predefinedPeriod;
}
