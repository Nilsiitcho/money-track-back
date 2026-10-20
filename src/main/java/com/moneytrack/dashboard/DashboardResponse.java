package com.moneytrack.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        String month,
        Totals totals,
        BigDecimal accumulated,
        List<SeriesPoint> series,
        List<CategorySlice> byCategory
) {
    public record Totals(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {
    }

    public record SeriesPoint(String month, BigDecimal saldo, BigDecimal accumulated) {
    }

    public record CategorySlice(Long categoryId, String name, BigDecimal total) {
    }
}
