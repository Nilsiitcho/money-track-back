package com.moneytrack.entry.domain;

import com.moneytrack.entry.EntryType;

import java.math.BigDecimal;

/** Valores editaveis de uma ocorrencia/regra. */
public record FieldValues(
        EntryType type,
        String description,
        BigDecimal amount,
        Long categoryId
) {
}
