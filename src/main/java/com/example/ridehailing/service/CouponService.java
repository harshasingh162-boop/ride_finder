package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.api.exception.InvalidCouponException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.discount.DiscountStrategy;
import com.example.ridehailing.discount.DiscountStrategyFactory;
import com.example.ridehailing.domain.Coupon;
import com.example.ridehailing.domain.CouponType;
import com.example.ridehailing.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final DiscountStrategyFactory discountStrategyFactory;
    private final Clock clock;

    public CouponService(CouponRepository couponRepository, DiscountStrategyFactory discountStrategyFactory,
                         Clock clock) {
        this.couponRepository = couponRepository;
        this.discountStrategyFactory = discountStrategyFactory;
        this.clock = clock;
    }

    public Coupon create(String code, CouponType type, int percent, BigDecimal maxDiscount, Instant validUntil) {
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 1 and 100, was " + percent);
        }
        if (maxDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxDiscount must be positive, was " + maxDiscount);
        }
        if (!validUntil.isAfter(clock.instant())) {
            throw new IllegalArgumentException("validUntil must be in the future, was " + validUntil);
        }
        couponRepository.findById(code).ifPresent(existing -> {
            throw new DuplicateException("coupon already exists: " + existing.code());
        });
        return couponRepository.save(new Coupon(code, type, percent, maxDiscount, validUntil, true));
    }

    public void delete(String code) {
        Coupon coupon = couponRepository.findById(code)
                .orElseThrow(() -> new NotFoundException("coupon not found: " + code));
        couponRepository.save(coupon.deactivated());
    }

    public DiscountStrategy resolve(String code) {
        Coupon coupon = couponRepository.findById(code)
                .orElseThrow(() -> new InvalidCouponException("unknown coupon: " + code));
        if (!coupon.active()) {
            throw new InvalidCouponException("coupon is no longer active: " + coupon.code());
        }
        if (coupon.isExpiredAt(clock.instant())) {
            throw new InvalidCouponException("coupon has expired: " + coupon.code());
        }
        return discountStrategyFactory.strategyFor(coupon);
    }

    public List<Coupon> findAll() {
        return couponRepository.findAll();
    }
}
