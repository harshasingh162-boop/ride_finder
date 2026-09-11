package com.example.ridehailing.pricing;

import com.example.ridehailing.domain.CarType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareCalculatorTest {

    private static final BigDecimal MINIMUM_FARE = new BigDecimal("50");
    private static final BigDecimal SURGE_CAP = new BigDecimal("2.0");

    private final PricingStrategy pricingStrategy = new TieredPricingStrategy(List.of(
            new PriceTier(new BigDecimal("2"), new BigDecimal("10")),
            new PriceTier(new BigDecimal("5"), new BigDecimal("8")),
            new PriceTier(null, new BigDecimal("5"))
    ));

    private ConditionsProvider conditionsProvider;
    private FareCalculator calculator;

    @BeforeEach
    void setUp() {
        conditionsProvider = new ConditionsProvider();
        calculator = new FareCalculator(pricingStrategy, List.of(new RainSurgeStrategy(), new TrafficSurgeStrategy()),
                conditionsProvider, MINIMUM_FARE, SURGE_CAP);
    }

    static Stream<Arguments> noSurgeTotals() {
        return Stream.of(
                Arguments.of(bd(1), CarType.HATCHBACK, bd("50.00")),
                Arguments.of(bd(6), CarType.HATCHBACK, bd("50.00")),
                Arguments.of(bd(7), CarType.HATCHBACK, bd("54.00")),
                Arguments.of(bd(10), CarType.HATCHBACK, bd("69.00")),
                Arguments.of(bd(10), CarType.SEDAN, bd("103.50")),
                Arguments.of(bd(4), CarType.HATCHBACK, bd("50.00")),
                Arguments.of(bd(4), CarType.SEDAN, bd("54.00"))
        );
    }

    @ParameterizedTest(name = "{0} km {1} -> {2}")
    @MethodSource("noSurgeTotals")
    void quotesTheExpectedTotalWithNoSurge(BigDecimal distanceKm, CarType carType, BigDecimal expectedTotal) {
        FareBreakdown breakdown = calculator.quote(distanceKm, carType);

        assertThat(breakdown.total()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void flagsMinimumFareAppliedWhenTieredFareIsWellBelowMinimum() {
        assertThat(calculator.quote(bd(1), CarType.HATCHBACK).minimumFareApplied()).isTrue();
    }

    @Test
    void flagsMinimumFareAppliedWhenTieredFareIsJustBelowMinimum() {
        assertThat(calculator.quote(bd(6), CarType.HATCHBACK).minimumFareApplied()).isTrue();
    }

    @Test
    void doesNotFlagMinimumFareAppliedWhenTieredFareExceedsMinimum() {
        assertThat(calculator.quote(bd(7), CarType.HATCHBACK).minimumFareApplied()).isFalse();
    }

    @Test
    void rainAloneAppliesA1Point2xSurgeOnTopOfTheTieredFare() {
        conditionsProvider.update(new RideConditions(true, TrafficLevel.LOW));

        assertThat(calculator.quote(bd(10), CarType.HATCHBACK).total()).isEqualByComparingTo(bd("82.80"));
    }

    @Test
    void rainAndHighTrafficMultiplyToAOnePointEightXSurge() {
        conditionsProvider.update(new RideConditions(true, TrafficLevel.HIGH));

        FareBreakdown breakdown = calculator.quote(bd(10), CarType.HATCHBACK);

        assertThat(breakdown.surgeMultiplier()).isEqualByComparingTo(bd("1.8"));
        assertThat(breakdown.total()).isEqualByComparingTo(bd("124.20"));
    }

    @Test
    void surgeAppliesOnTopOfAnAlreadyBumpedMinimumFare() {
        conditionsProvider.update(new RideConditions(true, TrafficLevel.LOW));

        assertThat(calculator.quote(bd(1), CarType.HATCHBACK).total()).isEqualByComparingTo(bd("60.00"));
    }

    @Test
    void surgeProductIsCappedAtTheConfiguredCap() {
        FareCalculator cappedCalculator = new FareCalculator(pricingStrategy, List.of(ctx -> new BigDecimal("3.0")),
                conditionsProvider, MINIMUM_FARE, SURGE_CAP);

        assertThat(cappedCalculator.quote(bd(10), CarType.HATCHBACK).total()).isEqualByComparingTo(bd("138.00"));
    }

    @Test
    void surgeBelowOneIsFlooredToOne() {
        FareCalculator flooredCalculator = new FareCalculator(pricingStrategy, List.of(ctx -> new BigDecimal("0.8")),
                conditionsProvider, MINIMUM_FARE, SURGE_CAP);

        assertThat(flooredCalculator.quote(bd(10), CarType.HATCHBACK).total()).isEqualByComparingTo(bd("69.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsNonPositiveDistance(String distanceKm) {
        assertThatThrownBy(() -> calculator.quote(new BigDecimal(distanceKm), CarType.HATCHBACK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullCarType() {
        assertThatThrownBy(() -> calculator.quote(bd(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
