package com.example.ridehailing.discount;

import java.math.BigDecimal;

public interface DiscountStrategy {

    BigDecimal discountFor(BigDecimal fare);
}
