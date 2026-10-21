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
 *
 * Suporta simulacao: {@code excluded} lista ids de lancamentos a desconsiderar
 * dos totais/saldo/acumulado. Os lancamentos ocultos continuam presentes nas
 * linhas da categoria (com seus proprios valores) para poderem ser reexibidos.
 */
public final class GridCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private GridCalculator() {
    }

    public static GridResponse build(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            Map<Long, String> names,
            Map<Long, String> kinds,
            YearMonth from,
            int months,
            Set<Long> excluded) {

        Set<Long> hidden = excluded == null ? Set.of() : excluded;
        Map<Long, String> catKinds = kinds == null ? Map.of() : kinds;

        List<YearMonth> cols = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            cols.add(from.plusMonths(i));
        }

        // Valores mes a mes por lancamento, com metadados da 1a ocorrencia na janela.
        Map<Long, BigDecimal[]> entryValues = new LinkedHashMap<>();
        Map<Long, EntryMeta> entryMeta = new HashMap<>();

        for (int idx = 0; idx < months; idx++) {
            List<Occurrence> occ = OccurrenceCalculator.occurrencesIn(entries, overrides, cols.get(idx));
            for (Occurrence o : occ) {
                entryValues.computeIfAbsent(o.entryId(), k -> newRow(months))[idx] =
                        entryValues.get(o.entryId())[idx].add(o.amount());
                entryMeta.computeIfAbsent(o.entryId(),
                        k -> new EntryMeta(o.type(), o.categoryId(), o.description()));
            }
        }

        // Agrupa lancamentos por categoria (receita/despesa) e soma apenas os visiveis.
        Map<Long, List<GridResponse.EntryRow>> recEntries = new HashMap<>();
        Map<Long, List<GridResponse.EntryRow>> despEntries = new HashMap<>();
        Map<Long, BigDecimal[]> recCat = new HashMap<>();
        Map<Long, BigDecimal[]> despCat = new HashMap<>();
        BigDecimal[] totRec = newRow(months);
        BigDecimal[] totDesp = newRow(months);

        for (Map.Entry<Long, BigDecimal[]> e : entryValues.entrySet()) {
            Long entryId = e.getKey();
            BigDecimal[] values = e.getValue();
            EntryMeta meta = entryMeta.get(entryId);
            boolean receita = meta.type() == EntryType.RECEITA;
            boolean visible = !hidden.contains(entryId);

            Map<Long, List<GridResponse.EntryRow>> entryTarget = receita ? recEntries : despEntries;
            Map<Long, BigDecimal[]> catTarget = receita ? recCat : despCat;
            BigDecimal[] total = receita ? totRec : totDesp;

            entryTarget.computeIfAbsent(meta.categoryId(), k -> new ArrayList<>())
                    .add(new GridResponse.EntryRow(entryId, meta.description(), Arrays.asList(values)));

            BigDecimal[] catRow = catTarget.computeIfAbsent(meta.categoryId(), k -> newRow(months));
            if (visible) {
                for (int idx = 0; idx < months; idx++) {
                    catRow[idx] = catRow[idx].add(values[idx]);
                    total[idx] = total[idx].add(values[idx]);
                }
            }
        }

        BigDecimal[] saldo = newRow(months);
        for (int idx = 0; idx < months; idx++) {
            saldo[idx] = totRec[idx].subtract(totDesp[idx]);
        }

        // acumulado comeca do acumulado real (sem os ocultos) ate o mes anterior a janela
        List<Entry> visibleEntries = entries.stream().filter(en -> !hidden.contains(en.getId())).toList();
        BigDecimal[] acc = newRow(months);
        BigDecimal running = BalanceCalculator.accumulatedUpTo(visibleEntries, overrides, from.minusMonths(1));
        for (int idx = 0; idx < months; idx++) {
            running = running.add(saldo[idx]);
            acc[idx] = running;
        }

        return new GridResponse(
                cols.stream().map(YearMonth::toString).toList(),
                toRows(recCat, recEntries, names, catKinds),
                toRows(despCat, despEntries, names, catKinds),
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

    private static List<GridResponse.CategoryRow> toRows(
            Map<Long, BigDecimal[]> catValues,
            Map<Long, List<GridResponse.EntryRow>> catEntries,
            Map<Long, String> names,
            Map<Long, String> kinds) {

        List<GridResponse.CategoryRow> rows = new ArrayList<>();
        for (Map.Entry<Long, List<GridResponse.EntryRow>> e : catEntries.entrySet()) {
            Long categoryId = e.getKey();
            List<GridResponse.EntryRow> entries = e.getValue();
            // mantem a categoria se algum lancamento (visivel ou oculto) tem valor
            boolean anyValue = entries.stream()
                    .flatMap(r -> r.values().stream())
                    .anyMatch(v -> v.signum() != 0);
            if (!anyValue) {
                continue;
            }
            entries.sort(Comparator.comparing(GridResponse.EntryRow::label, String.CASE_INSENSITIVE_ORDER));
            rows.add(new GridResponse.CategoryRow(
                    categoryId,
                    names.getOrDefault(categoryId, "?"),
                    kinds.getOrDefault(categoryId, "NORMAL"),
                    Arrays.asList(catValues.getOrDefault(categoryId, newRow(entries.get(0).values().size()))),
                    entries));
        }
        rows.sort(Comparator.comparing(GridResponse.CategoryRow::name, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /** Metadados de um lancamento (tipo/categoria/descricao) para montar as linhas. */
    private record EntryMeta(EntryType type, Long categoryId, String description) {
    }
}
