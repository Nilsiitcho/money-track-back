package com.moneytrack.admin;

/** Abrangencia do reset de dados. */
public enum ResetScope {
    /** Apaga apenas lancamentos (e suas excecoes por mes). Mantem categorias. */
    ENTRIES,
    /** Apaga lancamentos e todas as categorias (estado vazio). */
    ENTRIES_AND_CATEGORIES
}
