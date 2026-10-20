package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Logica pura de expansao de regras em ocorrencias. Sem dependencia de DB/Spring,
 * portanto testavel diretamente.
 */
public final class OccurrenceCalculator {

    private OccurrenceCalculator() {
    }

    /**
     * Resolve a ocorrencia de uma regra num mes especifico, aplicando override se existir.
     * Retorna vazio se a regra nao se aplica ao mes ou se foi removida por override.
     */
    public static Optional<Occurrence> resolve(Entry entry, EntryOverride override, YearMonth month) {
        YearMonth start = YearMonth.from(entry.getStartMonth());
        YearMonth end = entry.getEndMonth() == null ? null : YearMonth.from(entry.getEndMonth());

        boolean inRange = !month.isBefore(start) && (end == null || !month.isAfter(end));
        if (!inRange) {
            return Optional.empty();
        }
        if (override != null && override.isDeleted()) {
            return Optional.empty();
        }

        EntryType type = entry.getType();
        String description = entry.getDescription();
        BigDecimal amount = entry.getAmount();
        Long categoryId = entry.getCategoryId();
        if (override != null) {
            if (override.getType() != null) type = override.getType();
            if (override.getDescription() != null) description = override.getDescription();
            if (override.getAmount() != null) amount = override.getAmount();
            if (override.getCategoryId() != null) categoryId = override.getCategoryId();
        }

        Integer installmentNumber = null;
        Integer installmentTotal = null;
        if (entry.getRecurrence() == Recurrence.PARCELADO && entry.getInstallmentTotal() != null) {
            int first = entry.getInstallmentFirst() == null ? 1 : entry.getInstallmentFirst();
            installmentNumber = first + (int) start.until(month, java.time.temporal.ChronoUnit.MONTHS);
            installmentTotal = entry.getInstallmentTotal();
        }

        boolean scoped = entry.getRecurrence() != Recurrence.UNICO;

        return Optional.of(new Occurrence(
                entry.getId(), type, description, amount, categoryId, month,
                entry.getRecurrence(), installmentNumber, installmentTotal, scoped));
    }

    /**
     * Todas as ocorrencias de um mes para a lista de regras informada.
     *
     * @param overridesByEntry mapa entryId -> (mes -> override)
     */
    public static List<Occurrence> occurrencesIn(
            List<Entry> entries,
            Map<Long, Map<YearMonth, EntryOverride>> overridesByEntry,
            YearMonth month) {

        List<Occurrence> result = new ArrayList<>();
        for (Entry entry : entries) {
            Map<YearMonth, EntryOverride> perMonth = overridesByEntry.get(entry.getId());
            EntryOverride ov = perMonth == null ? null : perMonth.get(month);
            resolve(entry, ov, month).ifPresent(result::add);
        }
        return result;
    }
}
