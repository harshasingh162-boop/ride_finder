package com.example.ridehailing.discount;

import com.example.ridehailing.domain.Coupon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DiscountStrategyFactory {

    /**
     * Switch expression with no default so that adding a CouponType forces a
     * decision here at compile time.
     */
    public DiscountStrategy strategyFor(Coupon coupon) {
        return switch (coupon.type()) {
            case PERCENTAGE -> new PercentageDiscountStrategy(
                    BigDecimal.valueOf(coupon.percent()), coupon.maxDiscount());
        };
    }
}
