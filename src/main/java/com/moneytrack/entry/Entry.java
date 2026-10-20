package com.moneytrack.entry;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Definicao (regra) de um lancamento. Uma linha cobre UNICO, FIXO ou PARCELADO.
 * Meses sao armazenados como LocalDate no dia 1.
 */
@Entity
@Table(name = "entry")
@Getter
@Setter
@NoArgsConstructor
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Recurrence recurrence;

    /** Mes inicial (dia 1). */
    @Column(name = "start_month", nullable = false)
    private LocalDate startMonth;

    /** Mes final (dia 1). Null = indeterminado (FIXO). */
    @Column(name = "end_month")
    private LocalDate endMonth;

    /** Numero da primeira parcela da serie (default 1). Mantem continuidade apos split. */
    @Column(name = "installment_first")
    private Integer installmentFirst;

    /** Total de parcelas (somente PARCELADO). */
    @Column(name = "installment_total")
    private Integer installmentTotal;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
