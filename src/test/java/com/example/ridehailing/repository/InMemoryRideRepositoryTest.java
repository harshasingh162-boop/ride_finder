package com.example.ridehailing.repository;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.testsupport.TestRides;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRideRepositoryTest {

    private final InMemoryRideRepository repository = new InMemoryRideRepository();
    private final Location pickup = new Location(12.9716, 77.5946);
    private final Location drop = new Location(12.9352, 77.6245);
    private final Instant requestedAt = Instant.parse("2026-09-11T10:00:00Z");

    private Ride ride(String id, String userId, CarType carType) {
        return TestRides.ride(id, userId, pickup, drop, carType, requestedAt);
    }

    @Test
    void savedRideCanBeFoundById() {
        Ride ride = ride("ride-1", "user-1", CarType.HATCHBACK);

        repository.save(ride);

        assertThat(repository.findById("ride-1")).contains(ride);
    }

    @Test
    void findAllReturnsEverySavedRide() {
        Ride first = ride("ride-1", "user-1", CarType.HATCHBACK);
        Ride second = ride("ride-2", "user-2", CarType.SEDAN);

        repository.save(first);
        repository.save(second);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void aRiderWithOnlyQuotedRidesMayStartAnother() {
        repository.saveIfUserHasNoActiveRide(ride("ride-1", "user-1", CarType.HATCHBACK));

        assertThat(repository.saveIfUserHasNoActiveRide(ride("ride-2", "user-1", CarType.HATCHBACK))).isTrue();
    }

    @Test
    void aRiderOnAnActiveRideIsBlockedFromStartingAnother() {
        Ride first = ride("ride-1", "user-1", CarType.HATCHBACK);
        repository.saveIfUserHasNoActiveRide(first);
        first.transition(RideStatus.QUOTED, RideStatus.SEARCHING);

        boolean saved = repository.saveIfUserHasNoActiveRide(ride("ride-2", "user-1", CarType.HATCHBACK));

        assertThat(saved).isFalse();
        assertThat(repository.findById("ride-2")).isEmpty();
    }

    @Test
    void aDifferentRiderIsUnaffectedByAnActiveRide() {
        Ride first = ride("ride-1", "user-1", CarType.HATCHBACK);
        repository.saveIfUserHasNoActiveRide(first);
        first.transition(RideStatus.QUOTED, RideStatus.SEARCHING);

        assertThat(repository.saveIfUserHasNoActiveRide(ride("ride-2", "user-2", CarType.HATCHBACK))).isTrue();
    }

    @Test
    void aRiderWhoseRideCompletedMayStartAnother() {
        Ride first = ride("ride-1", "user-1", CarType.HATCHBACK);
        repository.saveIfUserHasNoActiveRide(first);
        first.transition(RideStatus.QUOTED, RideStatus.SEARCHING);
        first.transition(RideStatus.SEARCHING, RideStatus.DRIVER_ASSIGNED);
        first.transition(RideStatus.DRIVER_ASSIGNED, RideStatus.IN_PROGRESS);
        first.transition(RideStatus.IN_PROGRESS, RideStatus.COMPLETED);

        assertThat(repository.saveIfUserHasNoActiveRide(ride("ride-2", "user-1", CarType.HATCHBACK))).isTrue();
    }
}
