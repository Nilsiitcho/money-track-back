package com.moneytrack.entry.domain;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;

import java.util.List;

/**
 * Resultado de uma edicao com alcance. O servico persiste:
 * {@code master} (atualizar), {@code created} (inserir, se != null),
 * {@code override} (upsert, se != null) e reatribui {@code reassignedOverrides}
 * ao {@code created} apos ele ganhar id.
 */
public record EditPlan(
        Entry master,
        Entry created,
        EntryOverride override,
        List<EntryOverride> reassignedOverrides
) {
}
