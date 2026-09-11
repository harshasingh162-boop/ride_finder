package com.example.ridehailing.api;

import com.example.ridehailing.domain.Coupon;
import com.example.ridehailing.domain.CouponType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(String code, CouponType type, int percent, BigDecimal maxDiscount,
                             Instant validUntil, boolean active) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.code(), coupon.type(), coupon.percent(), coupon.maxDiscount(),
                coupon.validUntil(), coupon.active());
    }
}
