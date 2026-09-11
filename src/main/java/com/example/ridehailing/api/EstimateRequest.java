package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EstimateRequest(
        @NotNull @Valid LocationRequest pickup,
        @NotNull @Valid LocationRequest drop,
        @NotNull CarType carType,
        String couponCode
) {
}
