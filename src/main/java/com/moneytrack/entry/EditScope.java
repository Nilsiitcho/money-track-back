package com.moneytrack.entry;

public enum EditScope {
    /** Afeta apenas o mes de referencia. */
    ONLY_THIS,
    /** Afeta o mes de referencia e os seguintes. */
    THIS_AND_FUTURE,
    /** Afeta toda a serie. */
    ALL
}
