package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Fabricas de teste para entidades (sem DB). */
final class TestEntries {

    private TestEntries() {
    }

    static Entry unico(Long id, EntryType type, String amount, YearMonth month, Long categoryId) {
        Entry e = base(id, type, "unico", amount, Recurrence.UNICO, month, categoryId);
        e.setEndMonth(month.atDay(1));
        return e;
    }

    static Entry fixo(Long id, EntryType type, String amount, YearMonth start, Long categoryId) {
        Entry e = base(id, type, "fixo", amount, Recurrence.FIXO, start, categoryId);
        e.setEndMonth(null);
        return e;
    }

    static Entry parcelado(Long id, EntryType type, String amount, YearMonth start,
                           int total, Long categoryId) {
        Entry e = base(id, type, "parcelado", amount, Recurrence.PARCELADO, start, categoryId);
        e.setInstallmentFirst(1);
        e.setInstallmentTotal(total);
        e.setEndMonth(start.plusMonths(total - 1L).atDay(1));
        return e;
    }

    private static Entry base(Long id, EntryType type, String desc, String amount,
                              Recurrence rec, YearMonth start, Long categoryId) {
        Entry e = new Entry();
        e.setId(id);
        e.setUserId(1L);
        e.setCategoryId(categoryId);
        e.setType(type);
        e.setDescription(desc);
        e.setAmount(new BigDecimal(amount));
        e.setRecurrence(rec);
        e.setStartMonth(start.atDay(1));
        return e;
    }

    static EntryOverride override(Long entryId, YearMonth month) {
        EntryOverride ov = new EntryOverride();
        ov.setEntryId(entryId);
        ov.setReferenceMonth(month.atDay(1));
        return ov;
    }
}
