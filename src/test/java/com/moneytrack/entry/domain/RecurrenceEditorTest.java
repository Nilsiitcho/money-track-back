package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EditScope;
import com.moneytrack.entry.EntryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceEditorTest {

    private final YearMonth jan = YearMonth.of(2026, 1);
    private final YearMonth jun = YearMonth.of(2026, 6);

    private FieldValues newValues() {
        return new FieldValues(EntryType.DESPESA, "novo", new BigDecimal("999.00"), 99L);
    }

    // ---------- EDICAO ----------

    @Test
    void editOnlyThisCreatesOverrideLeavingMaster() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.ONLY_THIS, jun);

        assertThat(plan.created()).isNull();
        assertThat(plan.override()).isNotNull();
        assertThat(plan.override().getReferenceMonth()).isEqualTo(jun.atDay(1));
        assertThat(plan.override().getAmount()).isEqualByComparingTo("999.00");
        assertThat(plan.override().isDeleted()).isFalse();
        // master intacto
        assertThat(e.getAmount()).isEqualByComparingTo("100.00");
        assertThat(e.getEndMonth()).isNull();
    }

    @Test
    void editOnlyThisAtStartMonthStillCreatesOverride() {
        // Regressao: editar "somente este mes" no proprio mes inicial nao deve virar ALL
        Entry e = TestEntries.parcelado(1L, EntryType.DESPESA, "3000.00", jan, 3, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.ONLY_THIS, jan);

        assertThat(plan.created()).isNull();
        assertThat(plan.override()).isNotNull();
        assertThat(plan.override().getReferenceMonth()).isEqualTo(jan.atDay(1));
        assertThat(plan.override().getAmount()).isEqualByComparingTo("999.00");
        // master intacto: demais meses seguem 3000
        assertThat(e.getAmount()).isEqualByComparingTo("3000.00");
    }

    @Test
    void editAllMutatesMaster() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.ALL, jun);

        assertThat(plan.created()).isNull();
        assertThat(plan.override()).isNull();
        assertThat(e.getAmount()).isEqualByComparingTo("999.00");
        assertThat(e.getDescription()).isEqualTo("novo");
        assertThat(e.getCategoryId()).isEqualTo(99L);
    }

    @Test
    void editThisAndFutureSplitsFixo() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.THIS_AND_FUTURE, jun);

        // master encerra em maio
        assertThat(e.getEndMonth()).isEqualTo(YearMonth.of(2026, 5).atDay(1));
        // novo a partir de junho, indeterminado, com novos valores
        Entry created = plan.created();
        assertThat(created).isNotNull();
        assertThat(created.getStartMonth()).isEqualTo(jun.atDay(1));
        assertThat(created.getEndMonth()).isNull();
        assertThat(created.getAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void editThisAndFutureKeepsInstallmentNumbering() {
        // 10 parcelas desde janeiro; editar a partir de junho (parcela 6)
        Entry e = TestEntries.parcelado(1L, EntryType.DESPESA, "100.00", jan, 10, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.THIS_AND_FUTURE, jun);

        assertThat(e.getEndMonth()).isEqualTo(YearMonth.of(2026, 5).atDay(1));
        Entry created = plan.created();
        assertThat(created.getStartMonth()).isEqualTo(jun.atDay(1));
        assertThat(created.getInstallmentFirst()).isEqualTo(6);   // continua 6..10
        assertThat(created.getInstallmentTotal()).isEqualTo(10);
        assertThat(created.getEndMonth()).isEqualTo(YearMonth.of(2026, 10).atDay(1)); // fim original
    }

    @Test
    void editThisAndFutureWhenRefAtStartBehavesAsAll() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), EditScope.THIS_AND_FUTURE, jan);
        assertThat(plan.created()).isNull();
        assertThat(e.getAmount()).isEqualByComparingTo("999.00");
        assertThat(e.getEndMonth()).isNull();
    }

    @Test
    void editThisAndFutureReassignsLaterOverrides() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EntryOverride before = TestEntries.override(1L, YearMonth.of(2026, 3)); // fica
        EntryOverride after = TestEntries.override(1L, YearMonth.of(2026, 8));  // move
        EditPlan plan = RecurrenceEditor.edit(e, List.of(before, after),
                newValues(), EditScope.THIS_AND_FUTURE, jun);
        assertThat(plan.reassignedOverrides()).containsExactly(after);
    }

    @Test
    void editUnicoIgnoresScope() {
        Entry e = TestEntries.unico(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EditPlan plan = RecurrenceEditor.edit(e, List.of(), newValues(), null, jan);
        assertThat(plan.created()).isNull();
        assertThat(plan.override()).isNull();
        assertThat(e.getAmount()).isEqualByComparingTo("999.00");
    }

    // ---------- EXCLUSAO ----------

    @Test
    void deleteOnlyThisMarksOverrideDeleted() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        DeletePlan plan = RecurrenceEditor.delete(e, List.of(), EditScope.ONLY_THIS, jun);
        assertThat(plan.deleteMaster()).isFalse();
        assertThat(plan.override().isDeleted()).isTrue();
        assertThat(plan.override().getReferenceMonth()).isEqualTo(jun.atDay(1));
    }

    @Test
    void deleteThisAndFutureTruncatesAndRemovesLaterOverrides() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        EntryOverride before = TestEntries.override(1L, YearMonth.of(2026, 3));
        EntryOverride after = TestEntries.override(1L, YearMonth.of(2026, 7));
        DeletePlan plan = RecurrenceEditor.delete(e, List.of(before, after), EditScope.THIS_AND_FUTURE, jun);

        assertThat(plan.deleteMaster()).isFalse();
        assertThat(e.getEndMonth()).isEqualTo(YearMonth.of(2026, 5).atDay(1));
        assertThat(plan.overridesToDelete()).containsExactly(after);
    }

    @Test
    void deleteThisAndFutureAtStartDeletesMaster() {
        Entry e = TestEntries.fixo(1L, EntryType.DESPESA, "100.00", jan, 10L);
        DeletePlan plan = RecurrenceEditor.delete(e, List.of(), EditScope.THIS_AND_FUTURE, jan);
        assertThat(plan.deleteMaster()).isTrue();
    }

    @Test
    void deleteAllDeletesMaster() {
        Entry e = TestEntries.parcelado(1L, EntryType.DESPESA, "100.00", jan, 5, 10L);
        DeletePlan plan = RecurrenceEditor.delete(e, List.of(), EditScope.ALL, jun);
        assertThat(plan.deleteMaster()).isTrue();
    }

    @Test
    void deleteUnicoDeletesMaster() {
        Entry e = TestEntries.unico(1L, EntryType.DESPESA, "100.00", jan, 10L);
        DeletePlan plan = RecurrenceEditor.delete(e, List.of(), null, jan);
        assertThat(plan.deleteMaster()).isTrue();
    }
}
