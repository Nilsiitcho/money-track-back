package com.moneytrack.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tabela pivo: colunas = meses, linhas = categorias agrupadas por tipo, mais
 * linhas de totais, saldo do mes e saldo acumulado.
 */
public record GridResponse(
        List<String> months,
        List<CategoryRow> receitas,
        List<CategoryRow> despesas,
        List<BigDecimal> totalReceitas,
        List<BigDecimal> totalDespesas,
        List<BigDecimal> saldo,
        List<BigDecimal> accumulated
) {
    public record CategoryRow(Long categoryId, String name, List<BigDecimal> values) {
    }
}
