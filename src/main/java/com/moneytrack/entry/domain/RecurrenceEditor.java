package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EditScope;
import com.moneytrack.entry.Recurrence;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica pura de edicao/exclusao com alcance (ONLY_THIS / THIS_AND_FUTURE / ALL).
 * Nao toca em DB; retorna planos que o servico persiste.
 */
public final class RecurrenceEditor {

    private RecurrenceEditor() {
    }

    // ---------- EDICAO ----------

    public static EditPlan edit(Entry entry, List<EntryOverride> entryOverrides,
                                FieldValues v, EditScope scope, YearMonth ref) {

        if (entry.getRecurrence() == Recurrence.UNICO) {
            applyFields(entry, v);
            return new EditPlan(entry, null, null, List.of());
        }

        YearMonth start = YearMonth.from(entry.getStartMonth());

        if (scope == EditScope.ALL) {
            applyFields(entry, v);
            return new EditPlan(entry, null, null, List.of());
        }

        if (scope == EditScope.ONLY_THIS) {
            EntryOverride ov = findOverride(entryOverrides, ref);
            if (ov == null) {
                ov = new EntryOverride();
                ov.setEntryId(entry.getId());
                ov.setReferenceMonth(ref.atDay(1));
            }
            ov.setDeleted(false);
            ov.setType(v.type());
            ov.setDescription(v.description());
            ov.setAmount(v.amount());
            ov.setCategoryId(v.categoryId());
            return new EditPlan(entry, null, ov, List.of());
        }

        // THIS_AND_FUTURE: se o mes de referencia e o inicio (ou antes), equivale a ALL
        if (!ref.isAfter(start)) {
            applyFields(entry, v);
            return new EditPlan(entry, null, null, List.of());
        }

        // THIS_AND_FUTURE com ref > start => split
        java.time.LocalDate originalEnd = entry.getEndMonth();
        entry.setEndMonth(ref.minusMonths(1).atDay(1));

        Entry created = copyForSplit(entry, originalEnd, v, start, ref);

        List<EntryOverride> reassigned = new ArrayList<>();
        for (EntryOverride ov : entryOverrides) {
            if (!YearMonth.from(ov.getReferenceMonth()).isBefore(ref)) {
                reassigned.add(ov); // entryId sera trocado pelo servico apos created ter id
            }
        }
        return new EditPlan(entry, created, null, reassigned);
    }

    private static Entry copyForSplit(Entry original, java.time.LocalDate originalEnd,
                                      FieldValues v, YearMonth start, YearMonth ref) {
        Entry created = new Entry();
        created.setUserId(original.getUserId());
        created.setRecurrence(original.getRecurrence());
        created.setStartMonth(ref.atDay(1));
        // FIXO: segue indeterminado (null). PARCELADO: mantem o fim original.
        created.setEndMonth(original.getRecurrence() == Recurrence.FIXO ? null : originalEnd);
        if (original.getRecurrence() == Recurrence.PARCELADO) {
            int first = original.getInstallmentFirst() == null ? 1 : original.getInstallmentFirst();
            int offset = (int) start.until(ref, ChronoUnit.MONTHS);
            created.setInstallmentFirst(first + offset);
            created.setInstallmentTotal(original.getInstallmentTotal());
        }
        // valores
        created.setType(v.type());
        created.setDescription(v.description());
        created.setAmount(v.amount());
        created.setCategoryId(v.categoryId());
        return created;
    }

    private static void applyFields(Entry e, FieldValues v) {
        e.setType(v.type());
        e.setDescription(v.description());
        e.setAmount(v.amount());
        e.setCategoryId(v.categoryId());
    }

    // ---------- EXCLUSAO ----------

    public static DeletePlan delete(Entry entry, List<EntryOverride> entryOverrides,
                                    EditScope scope, YearMonth ref) {

        if (entry.getRecurrence() == Recurrence.UNICO || scope == EditScope.ALL) {
            return new DeletePlan(true, entry, null, List.of());
        }

        YearMonth start = YearMonth.from(entry.getStartMonth());

        if (scope == EditScope.ONLY_THIS) {
            EntryOverride ov = findOverride(entryOverrides, ref);
            if (ov == null) {
                ov = new EntryOverride();
                ov.setEntryId(entry.getId());
                ov.setReferenceMonth(ref.atDay(1));
            }
            ov.setDeleted(true);
            return new DeletePlan(false, entry, ov, List.of());
        }

        // THIS_AND_FUTURE
        if (!ref.isAfter(start)) {
            return new DeletePlan(true, entry, null, List.of());
        }
        entry.setEndMonth(ref.minusMonths(1).atDay(1));
        List<EntryOverride> toDelete = new ArrayList<>();
        for (EntryOverride ov : entryOverrides) {
            if (!YearMonth.from(ov.getReferenceMonth()).isBefore(ref)) {
                toDelete.add(ov);
            }
        }
        return new DeletePlan(false, entry, null, toDelete);
    }

    private static EntryOverride findOverride(List<EntryOverride> overrides, YearMonth ref) {
        for (EntryOverride ov : overrides) {
            if (YearMonth.from(ov.getReferenceMonth()).equals(ref)) {
                return ov;
            }
        }
        return null;
    }
}
