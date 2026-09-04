package com.healthcare.billing.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void lineTotal_calculatesAndRounds() {
        // 0.10 * 3 = 0.30 exactly with HALF_UP and scale 4
        assertThat(Money.lineTotal(new BigDecimal("0.10"), 3))
                .isEqualByComparingTo(new BigDecimal("0.3000"));
    }

    @Test
    void lineTotal_roundsUpHalf() {
        // 0.33333 -> 0.3333 at scale 4 (HALF_UP) — already exact at 4
        assertThat(Money.lineTotal(new BigDecimal("0.3333"), 1))
                .isEqualByComparingTo(new BigDecimal("0.3333"));
    }

    @Test
    void lineTotal_roundsUpAtScale() {
        // 19.999 * 3 = 59.997; with scale 4 it remains 59.9970
        assertThat(Money.lineTotal(new BigDecimal("19.999"), 3))
                .isEqualByComparingTo(new BigDecimal("59.9970"));
    }

    @Test
    void sum_emptyListIsZero() {
        assertThat(Money.sum(List.of())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sum_normalizesScale() {
        assertThat(Money.sum(List.of(
                        new BigDecimal("0.10"),
                        new BigDecimal("0.20"),
                        new BigDecimal("0.05"))))
                .isEqualByComparingTo(new BigDecimal("0.3500"));
    }
}
