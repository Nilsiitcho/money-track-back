package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryType;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceCalculatorTest {

    private final YearMonth aug = YearMonth.of(2026, 8);
    private final YearMonth sep = YearMonth.of(2026, 9);

    @Test
    void monthSaldoIsReceitasMinusDespesas() {
        List<Entry> entries = List.of(
                TestEntries.unico(1L, EntryType.RECEITA, "3000.00", aug, 1L),
                TestEntries.unico(2L, EntryType.DESPESA, "2000.00", aug, 2L));
        MonthBalance mb = BalanceCalculator.monthTotals(entries, Map.of(), aug);
        assertThat(mb.receitas()).isEqualByComparingTo("3000.00");
        assertThat(mb.despesas()).isEqualByComparingTo("2000.00");
        assertThat(mb.saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    void accumulatedSumsMonthlySaldos() {
        // ago: saldo 1000 ; set: saldo 1200 ; acumulado em set = 2200
        List<Entry> entries = List.of(
                TestEntries.unico(1L, EntryType.RECEITA, "1000.00", aug, 1L),
                TestEntries.unico(2L, EntryType.RECEITA, "1200.00", sep, 1L));

        assertThat(BalanceCalculator.accumulatedUpTo(entries, Map.of(), aug))
                .isEqualByComparingTo("1000.00");
        assertThat(BalanceCalculator.accumulatedUpTo(entries, Map.of(), sep))
                .isEqualByComparingTo("2200.00");
    }

    @Test
    void seriesCoversFromFirstMonthToSelected() {
        List<Entry> entries = List.of(
                TestEntries.unico(1L, EntryType.RECEITA, "1000.00", aug, 1L),
                TestEntries.unico(2L, EntryType.RECEITA, "1200.00", sep, 1L));
        List<MonthBalance> series = BalanceCalculator.series(entries, Map.of(), YearMonth.of(2026, 10));
        assertThat(series).hasSize(3); // ago, set, out
        assertThat(series.get(0).month()).isEqualTo(aug);
        assertThat(series.get(2).month()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(series.get(2).accumulated()).isEqualByComparingTo("2200.00"); // out sem lancamentos
    }

    @Test
    void futureMonthProjectsFixedAndInstallments() {
        // fixo de 100 desde jan e parcelado 3x de 50 desde jan
        List<Entry> entries = List.of(
                TestEntries.fixo(1L, EntryType.DESPESA, "100.00", YearMonth.of(2026, 1), 1L),
                TestEntries.parcelado(2L, EntryType.DESPESA, "50.00", YearMonth.of(2026, 1), 3, 1L));
        // mar: fixo(100) + parcela 3/3(50) = 150 despesa
        MonthBalance mar = BalanceCalculator.monthTotals(entries, Map.of(), YearMonth.of(2026, 3));
        assertThat(mar.despesas()).isEqualByComparingTo("150.00");
        // abr: so o fixo (parcelado terminou) = 100
        MonthBalance abr = BalanceCalculator.monthTotals(entries, Map.of(), YearMonth.of(2026, 4));
        assertThat(abr.despesas()).isEqualByComparingTo("100.00");
    }

    @Test
    void emptyWhenNoEntries() {
        assertThat(BalanceCalculator.series(List.of(), Map.of(), aug)).isEmpty();
        assertThat(BalanceCalculator.accumulatedUpTo(List.of(), Map.of(), aug))
                .isEqualByComparingTo("0.00");
    }
}
