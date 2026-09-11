package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.repository.InMemoryDriverRepository;
import com.example.ridehailing.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static com.example.ridehailing.testsupport.TestDrivers.offline;
import static com.example.ridehailing.testsupport.TestDrivers.onTrip;
import static org.assertj.core.api.Assertions.assertThat;

class LinearScanProximitySearchTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");
    private static final double RADIUS_KM = 2.0;
    private static final Duration STALENESS = Duration.ofSeconds(60);

    private MutableClock clock;
    private InMemoryDriverRepository driverRepository;
    private LinearScanProximitySearch search;

    @BeforeEach
    void setUp() {
        clock = MutableClock.startingAt(NOW);
        driverRepository = new InMemoryDriverRepository();
        search = new LinearScanProximitySearch(driverRepository, clock, RADIUS_KM, STALENESS);
    }

    private void store(Driver driver) {
        driverRepository.save(driver);
    }

    @Test
    void includesAnAvailableDriverJustInsideTheRadius() {
        store(available("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 1.9), NOW));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK)))
                .extracting(Driver::id).containsExactly("d1");
    }

    @Test
    void excludesADriverJustOutsideTheRadius() {
        store(available("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 2.1), NOW));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK))).isEmpty();
    }

    @Test
    void excludesOfflineDrivers() {
        store(offline("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK))).isEmpty();
    }

    @Test
    void excludesDriversAlreadyOnATrip() {
        store(onTrip("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK))).isEmpty();
    }

    @Test
    void includesADriverWhoseLastPingIsExactlyAtTheStalenessBoundary() {
        store(available("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));
        clock.advance(Duration.ofSeconds(60));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK)))
                .extracting(Driver::id).containsExactly("d1");
    }

    @Test
    void excludesADriverWhoseLastPingIsStale() {
        store(available("d1", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));
        clock.advance(Duration.ofSeconds(61));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK))).isEmpty();
    }

    @Test
    void excludesDriversWhoseCarTypeWasNotAskedFor() {
        store(available("hatch", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));
        store(available("sedan", CarType.SEDAN, 5.0, offsetNorth(BENGALURU, 0.6), NOW));

        assertThat(search.findNearby(BENGALURU, Set.of(CarType.SEDAN)))
                .extracting(Driver::id).containsExactly("sedan");
    }

    @Test
    void returnsEmptyWhenNoDriversAreRegistered() {
        assertThat(search.findNearby(BENGALURU, Set.of(CarType.HATCHBACK))).isEmpty();
    }
}
