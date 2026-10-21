package com.moneytrack.reserva;

import java.math.BigDecimal;

/**
 * Contexto do orcamento para montar o painel do lancamento rapido (antes de lancar).
 *
 * @param reserva        valor mensal da reserva (R)
 * @param daysInMonth    dias do mes (D)
 * @param daysRemaining  dias restantes a partir da data (Dr = D - dia + 1)
 * @param spentMonth     soma dos gastos rapidos ja lancados no mes (S)
 * @param remainingMonth R - S
 * @param dailyAvailable saldo diario disponivel = (R - S) / Dr (pode ser negativo)
 */
public record ReservaStatus(
        BigDecimal reserva,
        int daysInMonth,
        int daysRemaining,
        BigDecimal spentMonth,
        BigDecimal remainingMonth,
        BigDecimal dailyAvailable
) {
}
