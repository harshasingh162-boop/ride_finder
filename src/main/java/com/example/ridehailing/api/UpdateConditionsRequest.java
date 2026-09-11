package com.example.ridehailing.api;

import com.example.ridehailing.pricing.TrafficLevel;
import jakarta.validation.constraints.NotNull;

public record UpdateConditionsRequest(@NotNull Boolean raining, @NotNull TrafficLevel trafficLevel) {
}
