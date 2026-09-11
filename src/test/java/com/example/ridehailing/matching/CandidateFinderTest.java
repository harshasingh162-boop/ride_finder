package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.repository.InMemoryDriverRepository;
import com.example.ridehailing.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static org.assertj.core.api.Assertions.assertThat;

class CandidateFinderTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    private InMemoryDriverRepository driverRepository;
    private CandidateFinder candidateFinder;

    @BeforeEach
    void setUp() {
        driverRepository = new InMemoryDriverRepository();
        ProximitySearch search = new LinearScanProximitySearch(driverRepository,
                MutableClock.startingAt(NOW), 2.0, Duration.ofSeconds(60));
        candidateFinder = new CandidateFinder(search, new NearestDriverStrategy());
    }

    @Test
    void returnsHatchbacksWithoutUpgradingWhenSomeAreNearby() {
        driverRepository.save(available("hatch", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 1.0), NOW));
        driverRepository.save(available("sedan", CarType.SEDAN, 5.0, offsetNorth(BENGALURU, 0.5), NOW));

        MatchResult result = candidateFinder.find(BENGALURU, CarType.HATCHBACK);

        assertThat(result.rankedCandidates()).extracting(Driver::id).containsExactly("hatch");
        assertThat(result.assignedCarType()).isEqualTo(CarType.HATCHBACK);
        assertThat(result.upgraded()).isFalse();
    }

    @Test
    void upgradesToSedanWhenNoHatchbackIsNearby() {
        driverRepository.save(available("sedan", CarType.SEDAN, 5.0, offsetNorth(BENGALURU, 0.5), NOW));

        MatchResult result = candidateFinder.find(BENGALURU, CarType.HATCHBACK);

        assertThat(result.rankedCandidates()).extracting(Driver::id).containsExactly("sedan");
        assertThat(result.assignedCarType()).isEqualTo(CarType.SEDAN);
        assertThat(result.upgraded()).isTrue();
    }

    @Test
    void neverDowngradesASedanRequestToAHatchback() {
        driverRepository.save(available("hatch", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.5), NOW));

        MatchResult result = candidateFinder.find(BENGALURU, CarType.SEDAN);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.assignedCarType()).isEqualTo(CarType.SEDAN);
        assertThat(result.upgraded()).isFalse();
    }

    @Test
    void returnsAnEmptyResultRatherThanThrowingWhenNobodyIsAround() {
        MatchResult result = candidateFinder.find(BENGALURU, CarType.HATCHBACK);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.assignedCarType()).isEqualTo(CarType.HATCHBACK);
        assertThat(result.upgraded()).isFalse();
    }

    @Test
    void candidatesComeBackRankedByTheConfiguredStrategy() {
        driverRepository.save(available("far", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 1.8), NOW));
        driverRepository.save(available("near", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 0.3), NOW));

        MatchResult result = candidateFinder.find(BENGALURU, CarType.HATCHBACK);

        assertThat(result.rankedCandidates()).extracting(Driver::id).containsExactly("near", "far");
    }
}
