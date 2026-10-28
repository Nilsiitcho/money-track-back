package com.moneytrack.entry.dto;

import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
import com.moneytrack.entry.domain.Occurrence;

import java.math.BigDecimal;

public record OccurrenceResponse(
        Long entryId,
        EntryType type,
        String description,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        String month,
        Recurrence recurrence,
        String installmentLabel,
        boolean scoped,
        boolean quickEntry
) {
    public static OccurrenceResponse of(Occurrence o, String categoryName, boolean quickEntry) {
        return new OccurrenceResponse(
                o.entryId(), o.type(), o.description(), o.amount(),
                o.categoryId(), categoryName, o.month().toString(),
                o.recurrence(), o.installmentLabel(), o.scoped(), quickEntry);
    }
}
