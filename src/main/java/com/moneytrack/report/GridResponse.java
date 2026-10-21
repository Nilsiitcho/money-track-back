package com.moneytrack.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tabela pivo: colunas = meses, linhas = categorias agrupadas por tipo, mais
 * linhas de totais, saldo do mes e saldo acumulado.
 *
 * Cada categoria traz seus lancamentos ({@link EntryRow}) para permitir ocultar
 * registros individuais em simulacoes. Totais/saldo/acumulado ja vem calculados
 * desconsiderando os lancamentos ocultos informados na requisicao.
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
    public record CategoryRow(Long categoryId, String name, List<BigDecimal> values, List<EntryRow> entries) {
    }

    /** Lancamento individual dentro de uma categoria, com seus valores mes a mes. */
    public record EntryRow(Long entryId, String label, List<BigDecimal> values) {
    }
}
