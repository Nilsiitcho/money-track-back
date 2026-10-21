package com.moneytrack.report;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
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
                        k -> new EntryMeta(o.type(), o.categoryId(), o.description(), o.recurrence()));
            }
        }

        // Categoria especial Reserva (orcamento FIXO + gastos rapidos UNICO).
        Long reservaCatId = catKinds.entrySet().stream()
                .filter(en -> "RESERVA".equals(en.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        // Agrupa lancamentos por categoria (receita/despesa) e soma apenas os visiveis.
        Map<Long, List<GridResponse.EntryRow>> recEntries = new HashMap<>();
        Map<Long, List<GridResponse.EntryRow>> despEntries = new HashMap<>();
        Map<Long, BigDecimal[]> recCat = new HashMap<>();
        Map<Long, BigDecimal[]> despCat = new HashMap<>();
        BigDecimal[] totRec = newRow(months);
        BigDecimal[] totDesp = newRow(months);

        // Reserva: orcamento mensal (R) e gastos rapidos (S). Excedente = max(0, S-R).
        // display = sempre (mantem a linha visivel mesmo oculta); counted = so visivel (totais).
        BigDecimal[] reservaBudgetDisplay = newRow(months);
        BigDecimal[] reservaBudgetCounted = newRow(months);
        BigDecimal[] reservaSpend = newRow(months);
        List<GridResponse.EntryRow> reservaBudgetRows = new ArrayList<>();

        // Lancamentos sem categoria: cada um vira uma linha unica no seu grupo.
        List<GridResponse.CategoryRow> recSingles = new ArrayList<>();
        List<GridResponse.CategoryRow> despSingles = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal[]> e : entryValues.entrySet()) {
            Long entryId = e.getKey();
            BigDecimal[] values = e.getValue();
            EntryMeta meta = entryMeta.get(entryId);
            boolean visible = !hidden.contains(entryId);

            if (meta.categoryId() == null) {
                boolean receita = meta.type() == EntryType.RECEITA;
                GridResponse.EntryRow self =
                        new GridResponse.EntryRow(entryId, meta.description(), Arrays.asList(values));
                (receita ? recSingles : despSingles).add(new GridResponse.CategoryRow(
                        null, meta.description(), "SINGLE", Arrays.asList(values), List.of(self)));
                if (visible) {
                    BigDecimal[] total = receita ? totRec : totDesp;
                    for (int idx = 0; idx < months; idx++) {
                        total[idx] = total[idx].add(values[idx]);
                    }
                }
                continue;
            }

            if (reservaCatId != null && reservaCatId.equals(meta.categoryId())) {
                if (meta.recurrence() == Recurrence.FIXO) {
                    // orcamento da reserva (linha "Reserva"): display sempre, counted so se visivel
                    reservaBudgetRows.add(new GridResponse.EntryRow(entryId, meta.description(), Arrays.asList(values)));
                    for (int idx = 0; idx < months; idx++) {
                        reservaBudgetDisplay[idx] = reservaBudgetDisplay[idx].add(values[idx]);
                        if (visible) {
                            reservaBudgetCounted[idx] = reservaBudgetCounted[idx].add(values[idx]);
                        }
                    }
                } else {
                    // gasto rapido: consome a reserva (nao vira linha propria)
                    for (int idx = 0; idx < months; idx++) {
                        reservaSpend[idx] = reservaSpend[idx].add(values[idx]);
                    }
                }
                continue;
            }

            boolean receita = meta.type() == EntryType.RECEITA;
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

        // Excedente (vs orcamento real) + soma de reserva/excedente nas despesas.
        BigDecimal[] reservaExcess = newRow(months);
        boolean hasExcess = false;
        boolean hasBudget = false;
        for (int idx = 0; idx < months; idx++) {
            BigDecimal exc = reservaSpend[idx].subtract(reservaBudgetDisplay[idx]);
            if (exc.signum() > 0) {
                reservaExcess[idx] = exc;
                hasExcess = true;
            }
            if (reservaBudgetDisplay[idx].signum() != 0) {
                hasBudget = true;
            }
            // oculta a Reserva remove so o orcamento (counted) dos totais; excedente segue igual
            totDesp[idx] = totDesp[idx].add(reservaBudgetCounted[idx]).add(reservaExcess[idx]);
        }

        BigDecimal[] saldo = newRow(months);
        for (int idx = 0; idx < months; idx++) {
            saldo[idx] = totRec[idx].subtract(totDesp[idx]);
        }

        // Acumulado: soma dos saldos (reserva-aware) desde o 1o mes com lancamento
        // ate o mes anterior a janela, depois corre pela janela.
        List<Entry> visibleEntries = entries.stream().filter(en -> !hidden.contains(en.getId())).toList();
        BigDecimal[] acc = newRow(months);
        BigDecimal running = ZERO;
        Optional<YearMonth> first = BalanceCalculator.firstMonth(visibleEntries);
        if (first.isPresent()) {
            for (YearMonth m = first.get(); m.isBefore(from); m = m.plusMonths(1)) {
                running = running.add(reservaAwareSaldo(entries, overrides, reservaCatId, hidden, m));
            }
        }
        for (int idx = 0; idx < months; idx++) {
            running = running.add(saldo[idx]);
            acc[idx] = running;
        }

        // Receitas: categorias normais + linhas unicas (sem categoria).
        List<GridResponse.CategoryRow> receitasRows =
                new ArrayList<>(toRows(recCat, recEntries, names, catKinds));
        receitasRows.addAll(keepSingles(recSingles));

        // Despesas: categorias normais + linhas unicas + linha Reserva + linha Excedente.
        List<GridResponse.CategoryRow> despesasRows =
                new ArrayList<>(toRows(despCat, despEntries, names, catKinds));
        despesasRows.addAll(keepSingles(despSingles));
        if (reservaCatId != null && hasBudget) {
            reservaBudgetRows.sort(Comparator.comparing(GridResponse.EntryRow::label, String.CASE_INSENSITIVE_ORDER));
            despesasRows.add(new GridResponse.CategoryRow(
                    reservaCatId, names.getOrDefault(reservaCatId, "Reserva"), "RESERVA",
                    Arrays.asList(reservaBudgetDisplay), reservaBudgetRows));
        }
        if (hasExcess) {
            despesasRows.add(new GridResponse.CategoryRow(
                    -1L, "Excedente de Reserva", "RESERVA_EXCESS",
                    Arrays.asList(reservaExcess), List.of()));
        }

        return new GridResponse(
                cols.stream().map(YearMonth::toString).toList(),
                receitasRows,
                despesasRows,
                Arrays.asList(totRec),
                Arrays.asList(totDesp),
                Arrays.asList(saldo),
                Arrays.asList(acc));
    }

    /** Mantem apenas linhas unicas com algum valor; ordena por nome. */
    private static List<GridResponse.CategoryRow> keepSingles(List<GridResponse.CategoryRow> singles) {
        List<GridResponse.CategoryRow> out = new ArrayList<>();
        for (GridResponse.CategoryRow r : singles) {
            if (r.values().stream().anyMatch(v -> v.signum() != 0)) {
                out.add(r);
            }
        }
        out.sort(Comparator.comparing(GridResponse.CategoryRow::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Saldo de um mes aplicando a regra da Reserva (R + excedente), respeitando o olho. */
    private static BigDecimal reservaAwareSaldo(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            Long reservaCatId,
            Set<Long> hidden,
            YearMonth month) {

        BigDecimal rec = ZERO;
        BigDecimal desp = ZERO;
        BigDecimal budgetDisplay = ZERO;
        BigDecimal budgetCounted = ZERO;
        BigDecimal spend = ZERO;
        for (Occurrence o : OccurrenceCalculator.occurrencesIn(entries, overrides, month)) {
            boolean oHidden = hidden.contains(o.entryId());
            if (reservaCatId != null && reservaCatId.equals(o.categoryId())) {
                if (o.recurrence() == Recurrence.FIXO) {
                    budgetDisplay = budgetDisplay.add(o.amount());
                    if (!oHidden) {
                        budgetCounted = budgetCounted.add(o.amount());
                    }
                } else {
                    spend = spend.add(o.amount());
                }
            } else if (oHidden) {
                // lancamentos normais respeitam o olho
            } else if (o.type() == EntryType.RECEITA) {
                rec = rec.add(o.amount());
            } else {
                desp = desp.add(o.amount());
            }
        }
        BigDecimal reservaDesp = budgetCounted.add(spend.subtract(budgetDisplay).max(ZERO));
        return rec.subtract(desp).subtract(reservaDesp);
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

    /** Metadados de um lancamento (tipo/categoria/descricao/recorrencia) para montar as linhas. */
    private record EntryMeta(EntryType type, Long categoryId, String description, Recurrence recurrence) {
    }
}
