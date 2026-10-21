package com.moneytrack.report;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GridCalculatorTest {

    private Entry entry(Long id, EntryType type, String amount, Recurrence rec,
                        YearMonth start, YearMonth end, Long categoryId) {
        Entry e = new Entry();
        e.setId(id);
        e.setUserId(1L);
        e.setCategoryId(categoryId);
        e.setType(type);
        e.setDescription("x");
        e.setAmount(new BigDecimal(amount));
        e.setRecurrence(rec);
        e.setStartMonth(start.atDay(1));
        e.setEndMonth(end == null ? null : end.atDay(1));
        return e;
    }

    @Test
    void gridGroupsByCategoryAndAccumulatesFromHistory() {
        YearMonth aug = YearMonth.of(2026, 8);
        YearMonth oct = YearMonth.of(2026, 10);

        List<Entry> entries = List.of(
                // Salario fixo 5000 desde agosto (cat 1)
                entry(1L, EntryType.RECEITA, "5000.00", Recurrence.FIXO, aug, null, 1L),
                // Mercado unico 800 em outubro (cat 2)
                entry(2L, EntryType.DESPESA, "800.00", Recurrence.UNICO, oct, oct, 2L));
        Map<Long, String> names = Map.of(1L, "Salario", 2L, "Mercado");

        GridResponse g = GridCalculator.build(entries, Map.of(), names, Map.of(), oct, 3, Set.of());

        assertThat(g.months()).containsExactly("2026-10", "2026-11", "2026-12");

        // categoria expõe seus lançamentos individuais
        assertThat(g.receitas().get(0).entries()).hasSize(1);
        assertThat(g.receitas().get(0).entries().get(0).entryId()).isEqualTo(1L);

        assertThat(g.receitas()).hasSize(1);
        assertThat(g.receitas().get(0).name()).isEqualTo("Salario");
        assertThat(g.receitas().get(0).values()).containsExactly(
                bd("5000.00"), bd("5000.00"), bd("5000.00"));

        assertThat(g.despesas()).hasSize(1);
        assertThat(g.despesas().get(0).name()).isEqualTo("Mercado");
        assertThat(g.despesas().get(0).values()).containsExactly(bd("800.00"), bd("0.00"), bd("0.00"));

        assertThat(g.totalReceitas()).containsExactly(bd("5000.00"), bd("5000.00"), bd("5000.00"));
        assertThat(g.totalDespesas()).containsExactly(bd("800.00"), bd("0.00"), bd("0.00"));
        assertThat(g.saldo()).containsExactly(bd("4200.00"), bd("5000.00"), bd("5000.00"));

        // acumulado parte do historico anterior: ago(5000)+set(5000)=10000, depois +saldo de cada coluna
        assertThat(g.accumulated()).containsExactly(bd("14200.00"), bd("19200.00"), bd("24200.00"));
    }

    @Test
    void excludedEntryIsRemovedFromTotalsButStaysVisible() {
        YearMonth aug = YearMonth.of(2026, 8);
        YearMonth oct = YearMonth.of(2026, 10);

        List<Entry> entries = List.of(
                entry(1L, EntryType.RECEITA, "5000.00", Recurrence.FIXO, aug, null, 1L),
                // compra simulada: parcelado 200 x3 a partir de outubro (cat 2)
                parcela(2L, "200.00", oct, 2L),
                entry(3L, EntryType.DESPESA, "800.00", Recurrence.UNICO, oct, oct, 2L));
        Map<Long, String> names = Map.of(1L, "Salario", 2L, "Mercado");

        GridResponse g = GridCalculator.build(entries, Map.of(), names, Map.of(), oct, 3, Set.of(2L));

        // Mercado some do total o lançamento oculto (200), sobra só o de 800 em out
        assertThat(g.totalDespesas()).containsExactly(bd("800.00"), bd("0.00"), bd("0.00"));
        assertThat(g.saldo()).containsExactly(bd("4200.00"), bd("5000.00"), bd("5000.00"));

        // mas o lançamento oculto continua listado (para poder reexibir), com seus valores
        var mercado = g.despesas().stream().filter(c -> c.name().equals("Mercado")).findFirst().orElseThrow();
        var oculto = mercado.entries().stream().filter(e -> e.entryId().equals(2L)).findFirst().orElseThrow();
        assertThat(oculto.values()).containsExactly(bd("200.00"), bd("200.00"), bd("200.00"));
        // o valor da categoria (visível) não conta o oculto
        assertThat(mercado.values()).containsExactly(bd("800.00"), bd("0.00"), bd("0.00"));
    }

    @Test
    void reservaBudgetAndExcessLines() {
        YearMonth aug = YearMonth.of(2026, 8);
        YearMonth oct = YearMonth.of(2026, 10);
        Long reservaCat = 9L;

        List<Entry> entries = List.of(
                // orcamento FIXO 1200 desde agosto
                entry(10L, EntryType.DESPESA, "1200.00", Recurrence.FIXO, aug, null, reservaCat),
                // gastos rapidos de outubro: 1000 + 500 = 1500 -> excedente 300
                entry(11L, EntryType.DESPESA, "1000.00", Recurrence.UNICO, oct, oct, reservaCat),
                entry(12L, EntryType.DESPESA, "500.00", Recurrence.UNICO, oct, oct, reservaCat));
        Map<Long, String> names = Map.of(reservaCat, "Reserva");
        Map<Long, String> kinds = Map.of(reservaCat, "RESERVA");

        GridResponse g = GridCalculator.build(entries, Map.of(), names, kinds, oct, 3, Set.of());

        var reserva = g.despesas().stream().filter(c -> "RESERVA".equals(c.kind())).findFirst().orElseThrow();
        assertThat(reserva.values()).containsExactly(bd("1200.00"), bd("1200.00"), bd("1200.00"));

        var excedente = g.despesas().stream().filter(c -> "RESERVA_EXCESS".equals(c.kind())).findFirst().orElseThrow();
        assertThat(excedente.values()).containsExactly(bd("300.00"), bd("0.00"), bd("0.00"));

        // totais contam R + excedente (nao os rapidos crus)
        assertThat(g.totalDespesas()).containsExactly(bd("1500.00"), bd("1200.00"), bd("1200.00"));
        // acumulado inclui base ago+set (-1200 cada) e corre pela janela
        assertThat(g.accumulated()).containsExactly(bd("-3900.00"), bd("-5100.00"), bd("-6300.00"));
    }

    @Test
    void hiddenReservaKeepsRowButDropsFromTotals() {
        YearMonth aug = YearMonth.of(2026, 8);
        YearMonth oct = YearMonth.of(2026, 10);
        Long reservaCat = 9L;

        List<Entry> entries = List.of(
                entry(10L, EntryType.DESPESA, "1200.00", Recurrence.FIXO, aug, null, reservaCat));
        Map<Long, String> names = Map.of(reservaCat, "Reserva");
        Map<Long, String> kinds = Map.of(reservaCat, "RESERVA");

        // oculta o orcamento da reserva (id 10)
        GridResponse g = GridCalculator.build(entries, Map.of(), names, kinds, oct, 3, Set.of(10L));

        // a linha Reserva continua presente com seus valores (para o estilo "desabilitado")
        var reserva = g.despesas().stream().filter(c -> "RESERVA".equals(c.kind())).findFirst().orElseThrow();
        assertThat(reserva.values()).containsExactly(bd("1200.00"), bd("1200.00"), bd("1200.00"));
        // mas sai dos totais
        assertThat(g.totalDespesas()).containsExactly(bd("0.00"), bd("0.00"), bd("0.00"));
    }

    @Test
    void uncategorizedEntryBecomesSingleRow() {
        YearMonth oct = YearMonth.of(2026, 10);
        List<Entry> entries = List.of(
                entry(1L, EntryType.DESPESA, "250.00", Recurrence.UNICO, oct, oct, null));

        GridResponse g = GridCalculator.build(entries, Map.of(), Map.of(), Map.of(), oct, 2, Set.of());

        var single = g.despesas().stream().filter(c -> "SINGLE".equals(c.kind())).findFirst().orElseThrow();
        assertThat(single.categoryId()).isNull();
        assertThat(single.name()).isEqualTo("x");
        assertThat(single.values()).containsExactly(bd("250.00"), bd("0.00"));
        assertThat(single.entries()).hasSize(1);
        assertThat(g.totalDespesas()).containsExactly(bd("250.00"), bd("0.00"));
    }

    @Test
    void parceladoMarksFinalMonthOnLastInstallment() {
        YearMonth oct = YearMonth.of(2026, 10);
        List<Entry> entries = List.of(parcela(1L, "200.00", oct, 2L)); // PARCELADO 3x out-dez
        Map<Long, String> names = Map.of(2L, "Mercado");

        GridResponse g = GridCalculator.build(entries, Map.of(), names, Map.of(), oct, 3, Set.of());

        var e = g.despesas().get(0).entries().get(0);
        assertThat(e.finalMonth()).isEqualTo("2026-12"); // ultima parcela (3/3)
    }

    private Entry parcela(Long id, String amount, YearMonth start, Long categoryId) {
        Entry e = entry(id, EntryType.DESPESA, amount, Recurrence.PARCELADO, start, start.plusMonths(2), categoryId);
        e.setInstallmentFirst(1);
        e.setInstallmentTotal(3);
        return e;
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
