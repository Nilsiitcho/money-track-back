package com.moneytrack.entry.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthBalance(
        YearMonth month,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo,
        BigDecimal accumulated
) {
}
