package com.example.ridehailing.pricing;

import com.example.ridehailing.domain.Location;

public record SurgeContext(Location pickup, RideConditions conditions) {
}
