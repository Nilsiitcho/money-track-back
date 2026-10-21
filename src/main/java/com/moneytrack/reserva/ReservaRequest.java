package com.moneytrack.reserva;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Define o valor da Reserva de Gastos. amount = 0 desativa (remove o lancamento).
 * fromMonth (YYYY-MM) e opcional: mes inicial ao criar (default: mes atual).
 */
public record ReservaRequest(
        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.00", message = "Valor nao pode ser negativo")
        BigDecimal amount,
        String fromMonth
) {
}
