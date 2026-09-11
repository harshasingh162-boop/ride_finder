package com.example.ridehailing.repository;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.Ride;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRideRepositoryTest {

    private final InMemoryRideRepository repository = new InMemoryRideRepository();
    private final Location pickup = new Location(12.9716, 77.5946);
    private final Location drop = new Location(12.9352, 77.6245);
    private final Instant requestedAt = Instant.parse("2026-09-11T10:00:00Z");

    @Test
    void savedRideCanBeFoundById() {
        Ride ride = new Ride("ride-1", "user-1", pickup, drop, CarType.HATCHBACK, requestedAt);

        repository.save(ride);

        assertThat(repository.findById("ride-1")).contains(ride);
    }

    @Test
    void findAllReturnsEverySavedRide() {
        Ride first = new Ride("ride-1", "user-1", pickup, drop, CarType.HATCHBACK, requestedAt);
        Ride second = new Ride("ride-2", "user-2", pickup, drop, CarType.SEDAN, requestedAt);

        repository.save(first);
        repository.save(second);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById("missing")).isEmpty();
    }
}
