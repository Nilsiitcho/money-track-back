package com.moneytrack.entry.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.YearMonth;

public record EntryCreateRequest(
        @NotNull(message = "Tipo e obrigatorio")
        EntryType type,

        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String description,

        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.00", message = "Valor nao pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Valor invalido")
        BigDecimal amount,

        Long categoryId,

        @NotNull(message = "Recorrencia e obrigatoria")
        Recurrence recurrence,

        @NotNull(message = "Mes inicial e obrigatorio")
        @JsonFormat(pattern = "yyyy-MM")
        YearMonth startMonth,

        @JsonFormat(pattern = "yyyy-MM")
        YearMonth endMonth,

        @Min(value = 1, message = "Quantidade de parcelas deve ser >= 1")
        Integer installments
) {
}
