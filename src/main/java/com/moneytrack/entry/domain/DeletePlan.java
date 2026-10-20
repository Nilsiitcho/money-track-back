package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;

import java.util.List;

/**
 * Resultado de uma exclusao com alcance. Se {@code deleteMaster} e true, o servico
 * remove a regra (overrides caem por cascade). Caso contrario salva {@code master}
 * (fim ajustado), faz upsert de {@code override} e remove {@code overridesToDelete}.
 */
public record DeletePlan(
        boolean deleteMaster,
        Entry master,
        EntryOverride override,
        List<EntryOverride> overridesToDelete
) {
}
