package com.moneytrack.entry.dto;

import com.moneytrack.common.Months;
import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;

import java.math.BigDecimal;

/** Representa a regra/definicao do lancamento (nao uma ocorrencia). */
public record EntryResponse(
        Long id,
        EntryType type,
        String description,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        Recurrence recurrence,
        String startMonth,
        String endMonth,
        Integer installmentFirst,
        Integer installmentTotal
) {
    public static EntryResponse of(Entry e, String categoryName) {
        return new EntryResponse(
                e.getId(), e.getType(), e.getDescription(), e.getAmount(),
                e.getCategoryId(), categoryName, e.getRecurrence(),
                Months.format(Months.toYm(e.getStartMonth())),
                Months.format(Months.toYm(e.getEndMonth())),
                e.getInstallmentFirst(), e.getInstallmentTotal());
    }
}
