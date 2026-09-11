package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.api.exception.InvalidCouponException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.discount.DiscountStrategy;
import com.example.ridehailing.discount.DiscountStrategyFactory;
import com.example.ridehailing.domain.Coupon;
import com.example.ridehailing.domain.CouponType;
import com.example.ridehailing.repository.InMemoryCouponRepository;
import com.example.ridehailing.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");
    private static final Instant TOMORROW = NOW.plus(Duration.ofDays(1));

    private MutableClock clock;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        clock = MutableClock.startingAt(NOW);
        couponService = new CouponService(new InMemoryCouponRepository(), new DiscountStrategyFactory(), clock);
    }

    private Coupon createSave20() {
        return couponService.create("SAVE20", CouponType.PERCENTAGE, 20, new BigDecimal("30"), TOMORROW);
    }

    @Test
    void createdCouponIsActiveAndStoredUnderAnUpperCaseCode() {
        Coupon coupon = couponService.create("save20", CouponType.PERCENTAGE, 20, new BigDecimal("30"), TOMORROW);

        assertThat(coupon.code()).isEqualTo("SAVE20");
        assertThat(coupon.active()).isTrue();
    }

    @Test
    void resolveIsCaseInsensitive() {
        createSave20();

        DiscountStrategy strategy = couponService.resolve("save20");

        assertThat(strategy.discountFor(new BigDecimal("69"))).isEqualByComparingTo(new BigDecimal("13.80"));
    }

    @Test
    void findAllReturnsDeletedCouponsToo() {
        createSave20();
        couponService.create("HALF50", CouponType.PERCENTAGE, 50, new BigDecimal("30"), TOMORROW);
        couponService.delete("HALF50");

        assertThat(couponService.findAll())
                .extracting(Coupon::code, Coupon::active)
                .containsExactlyInAnyOrder(tuple("SAVE20", true), tuple("HALF50", false));
    }

    @Test
    void resolvingAnUnknownCodeIsRejected() {
        assertThatThrownBy(() -> couponService.resolve("NOPE"))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void resolvingADeletedCouponIsRejected() {
        createSave20();
        couponService.delete("SAVE20");

        assertThatThrownBy(() -> couponService.resolve("SAVE20"))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void resolvingAnExpiredCouponIsRejected() {
        createSave20();
        clock.advance(Duration.ofDays(2));

        assertThatThrownBy(() -> couponService.resolve("SAVE20"))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void couponStillResolvesRightUpToItsExpiryInstant() {
        createSave20();
        clock.advance(Duration.ofDays(1));

        assertThat(couponService.resolve("SAVE20")).isNotNull();
    }

    @Test
    void creatingADuplicateCodeIsRejectedRegardlessOfCase() {
        createSave20();

        assertThatThrownBy(() -> couponService.create("save20", CouponType.PERCENTAGE, 10,
                new BigDecimal("15"), TOMORROW))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void deletingAnUnknownCouponIsRejected() {
        assertThatThrownBy(() -> couponService.delete("NOPE"))
                .isInstanceOf(NotFoundException.class);
    }

    static Stream<Arguments> invalidCouponValues() {
        return Stream.of(
                Arguments.of("percent below range", 0, new BigDecimal("30"), Duration.ofDays(1)),
                Arguments.of("percent above range", 101, new BigDecimal("30"), Duration.ofDays(1)),
                Arguments.of("maxDiscount not positive", 20, BigDecimal.ZERO, Duration.ofDays(1)),
                Arguments.of("validUntil in the past", 20, new BigDecimal("30"), Duration.ofDays(-1))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCouponValues")
    void createRejectsInvalidValues(String description, int percent, BigDecimal maxDiscount, Duration validFor) {
        assertThatThrownBy(() -> couponService.create("CODE", CouponType.PERCENTAGE, percent, maxDiscount,
                NOW.plus(validFor)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
