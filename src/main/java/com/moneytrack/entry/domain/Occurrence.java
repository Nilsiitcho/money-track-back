package com.moneytrack.entry.domain;

import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Ocorrencia concreta de um lancamento num mes, ja com overrides aplicados.
 *
 * @param scoped true para FIXO/PARCELADO (edicao/exclusao pede alcance).
 */
public record Occurrence(
        Long entryId,
        EntryType type,
        String description,
        BigDecimal amount,
        Long categoryId,
        YearMonth month,
        Recurrence recurrence,
        Integer installmentNumber,
        Integer installmentTotal,
        boolean scoped
) {
    /** "X/N" para parcelados, null caso contrario. */
    public String installmentLabel() {
        if (installmentNumber == null || installmentTotal == null) {
            return null;
        }
        return installmentNumber + "/" + installmentTotal;
    }
}
