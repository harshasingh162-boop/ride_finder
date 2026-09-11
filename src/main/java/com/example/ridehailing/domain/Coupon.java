package com.example.ridehailing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable coupon. Codes are normalised to upper case on construction, which makes
 * every lookup case-insensitive without callers having to remember to fold the case.
 */
public record Coupon(String code, CouponType type, int percent, BigDecimal maxDiscount,
                     Instant validUntil, boolean active) {

    public Coupon {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(maxDiscount, "maxDiscount must not be null");
        Objects.requireNonNull(validUntil, "validUntil must not be null");
        code = normalizeCode(code);
    }

    public static String normalizeCode(String code) {
        return code.toUpperCase(Locale.ROOT);
    }

    /** Soft delete: a copy of this coupon that can no longer be resolved. */
    public Coupon deactivated() {
        return new Coupon(code, type, percent, maxDiscount, validUntil, false);
    }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(validUntil);
    }
}
