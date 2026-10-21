package com.moneytrack.reserva;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaServiceTest {

    @Test
    void dailyBudgetDividesAmountByDays() {
        // 3000 / 30 = 100.00
        assertThat(ReservaService.dailyBudget(new BigDecimal("3000.00"), 30))
                .isEqualByComparingTo("100.00");
        // 1000 / 31 = 32.26 (HALF_UP)
        assertThat(ReservaService.dailyBudget(new BigDecimal("1000.00"), 31))
                .isEqualByComparingTo("32.26");
    }

    @Test
    void dailyBudgetZeroForEmptyOrInvalid() {
        assertThat(ReservaService.dailyBudget(null, 30)).isEqualByComparingTo("0.00");
        assertThat(ReservaService.dailyBudget(BigDecimal.ZERO, 30)).isEqualByComparingTo("0.00");
        assertThat(ReservaService.dailyBudget(new BigDecimal("100.00"), 0)).isEqualByComparingTo("0.00");
    }
}
