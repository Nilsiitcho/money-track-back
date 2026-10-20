package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Logica pura de saldo mensal e saldo acumulado. Sem DB/Spring.
 */
public final class BalanceCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private BalanceCalculator() {
    }

    /** Primeiro mes com lancamento (menor start_month). Vazio se nao ha regras. */
    public static Optional<YearMonth> firstMonth(List<Entry> entries) {
        return entries.stream()
                .map(e -> YearMonth.from(e.getStartMonth()))
                .min(YearMonth::compareTo);
    }

    /** Totais de um unico mes (receitas, despesas, saldo). accumulated fica null aqui. */
    public static MonthBalance monthTotals(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            YearMonth month) {

        List<Occurrence> occ = OccurrenceCalculator.occurrencesIn(entries, overrides, month);
        BigDecimal receitas = ZERO;
        BigDecimal despesas = ZERO;
        for (Occurrence o : occ) {
            if (o.type() == EntryType.RECEITA) {
                receitas = receitas.add(o.amount());
            } else {
                despesas = despesas.add(o.amount());
            }
        }
        return new MonthBalance(month, receitas, despesas, receitas.subtract(despesas), null);
    }

    /**
     * Serie mes a mes de {@code first} ate {@code selected} (inclusive), com saldo
     * acumulado corrente. Se nao ha regras, retorna lista vazia.
     */
    public static List<MonthBalance> series(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            YearMonth selected) {

        Optional<YearMonth> first = firstMonth(entries);
        if (first.isEmpty()) {
            return List.of();
        }
        YearMonth start = first.get();
        // Se o mes selecionado for anterior ao primeiro, a serie fica vazia.
        YearMonth end = selected;

        List<MonthBalance> out = new ArrayList<>();
        BigDecimal acc = ZERO;
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            MonthBalance mb = monthTotals(entries, overrides, m);
            acc = acc.add(mb.saldo());
            out.add(new MonthBalance(m, mb.receitas(), mb.despesas(), mb.saldo(), acc));
            if (m.equals(end)) break;
        }
        return out;
    }

    /** Saldo acumulado ate o mes selecionado (inclusive). */
    public static BigDecimal accumulatedUpTo(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overrides,
            YearMonth selected) {

        List<MonthBalance> s = series(entries, overrides, selected);
        if (s.isEmpty()) {
            return ZERO;
        }
        return s.get(s.size() - 1).accumulated();
    }
}
