package com.moneytrack.entry;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Excecao de uma regra para um mes especifico (editar/excluir "somente este mes").
 * Campos nulos nao sobrescrevem; deleted=true remove a ocorrencia naquele mes.
 */
@Entity
@Table(name = "entry_override")
@Getter
@Setter
@NoArgsConstructor
public class EntryOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private EntryType type;

    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
