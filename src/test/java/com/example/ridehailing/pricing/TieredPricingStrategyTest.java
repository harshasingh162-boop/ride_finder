package com.example.ridehailing.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TieredPricingStrategyTest {

    private static final List<PriceTier> STANDARD_TIERS = List.of(
            new PriceTier(bd(2), bd(10)),
            new PriceTier(bd(5), bd(8)),
            new PriceTier(null, bd(5))
    );

    private final TieredPricingStrategy strategy = new TieredPricingStrategy(STANDARD_TIERS);

    static Stream<Arguments> baseFares() {
        return Stream.of(
                Arguments.of(bd(2), bd("20.00")),
                Arguments.of(bd("2.5"), bd("24.00")),
                Arguments.of(bd(5), bd("44.00")),
                Arguments.of(bd("5.5"), bd("46.50")),
                Arguments.of(bd(10), bd("69.00")),
                Arguments.of(bd(12), bd("79.00"))
        );
    }

    @ParameterizedTest(name = "{0} km -> {1}")
    @MethodSource("baseFares")
    void computesTheTieredBaseFare(BigDecimal distanceKm, BigDecimal expected) {
        assertThat(strategy.baseFare(distanceKm)).isEqualByComparingTo(expected);
    }

    @Test
    void rejectsUnsortedTiers() {
        assertThatThrownBy(() -> new TieredPricingStrategy(List.of(
                new PriceTier(bd(5), bd(10)),
                new PriceTier(bd(2), bd(8)),
                new PriceTier(null, bd(5))
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOverlappingTiers() {
        assertThatThrownBy(() -> new TieredPricingStrategy(List.of(
                new PriceTier(bd(2), bd(10)),
                new PriceTier(bd(2), bd(8)),
                new PriceTier(null, bd(5))
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsConfigWithNoOpenEndedTier() {
        assertThatThrownBy(() -> new TieredPricingStrategy(List.of(
                new PriceTier(bd(2), bd(10)),
                new PriceTier(bd(5), bd(8))
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAZeroRate() {
        assertThatThrownBy(() -> new TieredPricingStrategy(List.of(
                new PriceTier(bd(2), BigDecimal.ZERO),
                new PriceTier(null, bd(5))
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
