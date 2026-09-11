package com.example.ridehailing.api;

import com.example.ridehailing.domain.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Only presence is validated here. The value rules (percent 1..100, positive maxDiscount,
 * validUntil in the future) live in CouponService because they need the injected Clock and
 * belong with the rest of the coupon invariants.
 */
public record CreateCouponRequest(
        @NotBlank String code,
        @NotNull CouponType type,
        @NotNull Integer percent,
        @NotNull BigDecimal maxDiscount,
        @NotNull Instant validUntil
) {
}
