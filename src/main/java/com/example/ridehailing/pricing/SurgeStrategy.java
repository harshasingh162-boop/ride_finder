package com.example.ridehailing.pricing;

import java.math.BigDecimal;

public interface SurgeStrategy {

    BigDecimal multiplier(SurgeContext ctx);
}
