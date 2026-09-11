package com.example.ridehailing.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RideTest {

    private final Location pickup = new Location(12.9716, 77.5946);
    private final Location drop = new Location(12.9352, 77.6245);

    private Ride newRide() {
        return new Ride("ride-1", "user-1", pickup, drop, CarType.HATCHBACK, Instant.parse("2026-09-11T10:00:00Z"));
    }

    @Test
    void transitionSucceedsWhenFromStateMatchesCurrentState() {
        Ride ride = newRide();

        boolean transitioned = ride.transition(RideStatus.QUOTED, RideStatus.SEARCHING);

        assertThat(transitioned).isTrue();
        assertThat(ride.status()).isEqualTo(RideStatus.SEARCHING);
    }

    @Test
    void transitionFailsAndLeavesStatusUnchangedWhenFromStateDoesNotMatch() {
        Ride ride = newRide();

        boolean transitioned = ride.transition(RideStatus.SEARCHING, RideStatus.IN_PROGRESS);

        assertThat(transitioned).isFalse();
        assertThat(ride.status()).isEqualTo(RideStatus.QUOTED);
    }
}
