package com.moneytrack.report;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.domain.BalanceCalculator;
import com.moneytrack.entry.domain.Occurrence;
import com.moneytrack.entry.domain.OccurrenceCalculator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

/**
 * Logica pura da tabela pivo (meses x categorias). Sem DB/Spring.
 */
public final class GridCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private GridCalculator() {
    }

    public static GridResponse build(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            Map<Long, String> names,
            YearMonth from,
            int months) {

        List<YearMonth> cols = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            cols.add(from.plusMonths(i));
        }

        Map<Long, BigDecimal[]> receitas = new HashMap<>();
        Map<Long, BigDecimal[]> despesas = new HashMap<>();
        BigDecimal[] totRec = newRow(months);
        BigDecimal[] totDesp = newRow(months);
        BigDecimal[] saldo = newRow(months);

        for (int idx = 0; idx < months; idx++) {
            List<Occurrence> occ = OccurrenceCalculator.occurrencesIn(entries, overrides, cols.get(idx));
            for (Occurrence o : occ) {
                boolean receita = o.type() == EntryType.RECEITA;
                Map<Long, BigDecimal[]> target = receita ? receitas : despesas;
                target.computeIfAbsent(o.categoryId(), k -> newRow(months))[idx] =
                        target.get(o.categoryId())[idx].add(o.amount());
                if (receita) {
                    totRec[idx] = totRec[idx].add(o.amount());
                } else {
                    totDesp[idx] = totDesp[idx].add(o.amount());
                }
            }
            saldo[idx] = totRec[idx].subtract(totDesp[idx]);
        }

        // acumulado comeca do acumulado real ate o mes anterior a janela
        BigDecimal[] acc = newRow(months);
        BigDecimal running = BalanceCalculator.accumulatedUpTo(entries, overrides, from.minusMonths(1));
        for (int idx = 0; idx < months; idx++) {
            running = running.add(saldo[idx]);
            acc[idx] = running;
        }

        return new GridResponse(
                cols.stream().map(YearMonth::toString).toList(),
                toRows(receitas, names),
                toRows(despesas, names),
                Arrays.asList(totRec),
                Arrays.asList(totDesp),
                Arrays.asList(saldo),
                Arrays.asList(acc));
    }

    private static BigDecimal[] newRow(int n) {
        BigDecimal[] row = new BigDecimal[n];
        Arrays.fill(row, ZERO);
        return row;
    }

    private static List<GridResponse.CategoryRow> toRows(Map<Long, BigDecimal[]> map, Map<Long, String> names) {
        List<GridResponse.CategoryRow> rows = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> e : map.entrySet()) {
            if (Arrays.stream(e.getValue()).noneMatch(v -> v.signum() != 0)) {
                continue;
            }
            rows.add(new GridResponse.CategoryRow(
                    e.getKey(), names.getOrDefault(e.getKey(), "?"), Arrays.asList(e.getValue())));
        }
        rows.sort(Comparator.comparing(GridResponse.CategoryRow::name, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }
}
