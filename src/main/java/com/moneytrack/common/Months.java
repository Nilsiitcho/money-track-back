package com.moneytrack.common;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

public final class Months {

    private Months() {
    }

    public static YearMonth parse(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("Mes e obrigatorio (formato YYYY-MM)");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeException ex) {
            throw ApiException.badRequest("Mes invalido '" + value + "' (use YYYY-MM)");
        }
    }

    public static LocalDate toDate(YearMonth ym) {
        return ym == null ? null : ym.atDay(1);
    }

    public static YearMonth toYm(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }

    public static String format(YearMonth ym) {
        return ym == null ? null : ym.toString();
    }
}
