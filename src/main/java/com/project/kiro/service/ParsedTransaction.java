package com.project.kiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
    BigDecimal amount,
    TransactionType type,
    LocalDate date,
    String merchant
) {}
