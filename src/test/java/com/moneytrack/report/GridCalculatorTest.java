package com.moneytrack.report;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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

        GridResponse g = GridCalculator.build(entries, Map.of(), names, oct, 3);

        assertThat(g.months()).containsExactly("2026-10", "2026-11", "2026-12");

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

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
