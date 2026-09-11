package com.example.ridehailing.discount;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PercentageDiscountStrategyTest {

    private final DiscountStrategy save20 =
            new PercentageDiscountStrategy(new BigDecimal("20"), new BigDecimal("30"));

    @Test
    void takesThePercentageWhenItIsBelowTheCap() {
        assertThat(save20.discountFor(new BigDecimal("69"))).isEqualByComparingTo(new BigDecimal("13.80"));
    }

    @Test
    void capsTheDiscountAtTheConfiguredMaximum() {
        assertThat(save20.discountFor(new BigDecimal("1000"))).isEqualByComparingTo(new BigDecimal("30"));
    }

    @Test
    void discountIsZeroForAZeroFare() {
        assertThat(save20.discountFor(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
