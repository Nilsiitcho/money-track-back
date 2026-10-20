package com.moneytrack.entry;

public enum Recurrence {
    /** Ocorre apenas no mes informado. */
    UNICO,
    /** Repete todo mes a partir do mes inicial, por tempo indeterminado. */
    FIXO,
    /** Repete do mes inicial ate o mes de termino (parcelado). */
    PARCELADO
}
