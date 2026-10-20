package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrenceCalculatorTest {

    private final YearMonth jan = YearMonth.of(2026, 1);
    private final YearMonth mar = YearMonth.of(2026, 3);
    private final YearMonth jul = YearMonth.of(2026, 7);
    private final YearMonth aug = YearMonth.of(2026, 8);

    @Test
    void unicoAppearsOnlyInItsMonth() {
        Entry e = TestEntries.unico(1L, EntryType.DESPESA, "100.00", mar, 10L);
        assertThat(OccurrenceCalculator.resolve(e, null, mar)).isPresent();
        assertThat(OccurrenceCalculator.resolve(e, null, jan)).isEmpty();
        assertThat(OccurrenceCalculator.resolve(e, null, jul)).isEmpty();
    }

    @Test
    void fixoRepeatsEveryMonthFromStart() {
        Entry e = TestEntries.fixo(1L, EntryType.RECEITA, "5000.00", mar, 10L);
        assertThat(OccurrenceCalculator.resolve(e, null, jan)).isEmpty();
        assertThat(OccurrenceCalculator.resolve(e, null, mar)).isPresent();
        assertThat(OccurrenceCalculator.resolve(e, null, YearMonth.of(2030, 12))).isPresent();
    }

    @Test
    void parceladoSpansStartToEndAndNumbersInstallments() {
        // 5 parcelas a partir de marco => mar..jul
        Entry e = TestEntries.parcelado(1L, EntryType.DESPESA, "200.00", mar, 5, 10L);

        assertThat(OccurrenceCalculator.resolve(e, null, YearMonth.of(2026, 2))).isEmpty();
        assertThat(OccurrenceCalculator.resolve(e, null, aug)).isEmpty();

        Occurrence first = OccurrenceCalculator.resolve(e, null, mar).orElseThrow();
        assertThat(first.installmentNumber()).isEqualTo(1);
        assertThat(first.installmentTotal()).isEqualTo(5);
        assertThat(first.installmentLabel()).isEqualTo("1/5");

        Occurrence last = OccurrenceCalculator.resolve(e, null, jul).orElseThrow();
        assertThat(last.installmentLabel()).isEqualTo("5/5");
    }

    @Test
    void parceladoWithInstallmentFirstOffsetKeepsNumbering() {
        // serie partida: comeca na parcela 3 de 5, a partir de maio
        Entry e = TestEntries.parcelado(1L, EntryType.DESPESA, "200.00", YearMonth.of(2026, 5), 5, 10L);
        e.setInstallmentFirst(3);
        e.setInstallmentTotal(5);
        Occurrence o = OccurrenceCalculator.resolve(e, null, YearMonth.of(2026, 5)).orElseThrow();
        assertThat(o.installmentLabel()).isEqualTo("3/5");
        Occurrence o2 = OccurrenceCalculator.resolve(e, null, YearMonth.of(2026, 6)).orElseThrow();
        assertThat(o2.installmentLabel()).isEqualTo("4/5");
    }

    @Test
    void overrideReplacesFieldsAndDeleteSkips() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", mar, 10L);

        EntryOverride edit = TestEntries.override(1L, jul);
        edit.setAmount(new BigDecimal("250.00"));
        edit.setDescription("ajustado");
        Occurrence o = OccurrenceCalculator.resolve(e, edit, jul).orElseThrow();
        assertThat(o.amount()).isEqualByComparingTo("250.00");
        assertThat(o.description()).isEqualTo("ajustado");

        EntryOverride del = TestEntries.override(1L, aug);
        del.setDeleted(true);
        assertThat(OccurrenceCalculator.resolve(e, del, aug)).isEmpty();
    }

    @Test
    void occurrencesInMonthCollectsAllRules() {
        Entry salario = TestEntries.fixo(1L, EntryType.RECEITA, "5000.00", jan, 1L);
        Entry mercado = TestEntries.unico(2L, EntryType.DESPESA, "300.00", mar, 2L);
        List<Occurrence> inMar = OccurrenceCalculator.occurrencesIn(
                List.of(salario, mercado), Map.of(), mar);
        assertThat(inMar).hasSize(2);
        List<Occurrence> inFeb = OccurrenceCalculator.occurrencesIn(
                List.of(salario, mercado), Map.of(), YearMonth.of(2026, 2));
        assertThat(inFeb).hasSize(1);
    }
}
