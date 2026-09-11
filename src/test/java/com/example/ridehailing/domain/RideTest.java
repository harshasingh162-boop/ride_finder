package com.example.ridehailing.domain;

import com.example.ridehailing.testsupport.TestRides;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RideTest {

    private final Location pickup = new Location(12.9716, 77.5946);
    private final Location drop = new Location(12.9352, 77.6245);

    private Ride newRide() {
        return TestRides.ride("ride-1", "user-1", pickup, drop, CarType.HATCHBACK,
                Instant.parse("2026-09-11T10:00:00Z"));
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

    @Test
    void assigningADriverRecordsAnUpgradeWhenTheCarTypeDiffersFromTheRequest() {
        Ride ride = newRide();
        Driver driver = new Driver("driver-1", "Kiran", "+91-900",
                new Car("KA-01", "Dzire", CarType.SEDAN));

        ride.assignTo(driver, Instant.parse("2026-09-11T10:05:00Z"));

        assertThat(ride.driverId()).isEqualTo("driver-1");
        assertThat(ride.assignedCarType()).isEqualTo(CarType.SEDAN);
        assertThat(ride.upgraded()).isTrue();
    }

    @Test
    void assigningADriverOfTheRequestedTypeIsNotAnUpgrade() {
        Ride ride = newRide();
        Driver driver = new Driver("driver-1", "Kiran", "+91-900",
                new Car("KA-01", "Swift", CarType.HATCHBACK));

        ride.assignTo(driver, Instant.parse("2026-09-11T10:05:00Z"));

        assertThat(ride.upgraded()).isFalse();
    }
}
