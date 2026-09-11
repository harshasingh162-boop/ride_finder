package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Deliberately separate from EstimateRequest even though the fields coincide today: booking a
 * ride and pricing one are different contracts and will diverge (payment method, scheduling).
 */
public record RequestRideRequest(
        @NotNull @Valid LocationRequest pickup,
        @NotNull @Valid LocationRequest drop,
        @NotNull CarType carType,
        String couponCode
) {
}
