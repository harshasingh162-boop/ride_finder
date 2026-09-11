package com.example.ridehailing.service;

import com.example.ridehailing.discount.DiscountStrategy;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.pricing.FareBreakdown;
import com.example.ridehailing.pricing.FareCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class MatchingService {

    private final FareCalculator fareCalculator;
    private final CouponService couponService;

    public MatchingService(FareCalculator fareCalculator, CouponService couponService) {
        this.fareCalculator = fareCalculator;
        this.couponService = couponService;
    }

    public FareBreakdown estimate(Location pickup, Location drop, CarType carType, String couponCode) {
        if (pickup.equals(drop)) {
            throw new IllegalArgumentException("pickup and drop must be different locations");
        }
        BigDecimal distanceKm = BigDecimal.valueOf(pickup.distanceKmTo(drop));
        Optional<DiscountStrategy> discountStrategy = couponCode == null || couponCode.isBlank()
                ? Optional.empty()
                : Optional.of(couponService.resolve(couponCode));
        return fareCalculator.quote(distanceKm, carType, discountStrategy);
    }
}
