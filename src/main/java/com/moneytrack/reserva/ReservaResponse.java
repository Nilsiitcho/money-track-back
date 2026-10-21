package com.moneytrack.reserva;

import java.math.BigDecimal;

/**
 * Estado da Reserva de Gastos para um mes:
 * valor mensal (lancado como FIXO) e o orcamento diario = valor / dias do mes.
 */
public record ReservaResponse(
        Long categoryId,
        BigDecimal amount,
        String month,
        int daysInMonth,
        BigDecimal dailyBudget
) {
}
