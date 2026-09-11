package com.example.ridehailing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LocationTest {

    @Test
    void distanceBetweenPointsOneDegreeOfLatitudeApartIsApproximately111Km() {
        Location a = new Location(0.0, 0.0);
        Location b = new Location(1.0, 0.0);

        assertThat(a.distanceKmTo(b)).isCloseTo(111.19, within(0.01));
    }

    @Test
    void distanceBetweenSamePointIsZero() {
        Location a = new Location(12.9716, 77.5946);

        assertThat(a.distanceKmTo(a)).isEqualTo(0.0);
    }

    @Test
    void latitudeAboveNinetyIsRejected() {
        assertThatThrownBy(() -> new Location(91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longitudeBelowNegative180IsRejected() {
        assertThatThrownBy(() -> new Location(0.0, -181.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
