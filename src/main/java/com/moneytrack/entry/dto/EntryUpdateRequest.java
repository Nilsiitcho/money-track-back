package com.moneytrack.entry.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moneytrack.entry.EditScope;
import com.moneytrack.entry.EntryType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Edita os valores de um lancamento. Para FIXO/PARCELADO, {@code scope} define o
 * alcance e {@code referenceMonth} indica o mes tocado (exceto scope ALL).
 * Mudancas estruturais (recorrencia, datas, parcelas) nao sao suportadas aqui;
 * exclua e recrie.
 */
public record EntryUpdateRequest(
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

        EditScope scope,

        @JsonFormat(pattern = "yyyy-MM")
        YearMonth referenceMonth
) {
}
