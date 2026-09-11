package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static org.assertj.core.api.Assertions.assertThat;

class DriverMatchingStrategyTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    private final Driver near = available("near", CarType.HATCHBACK, 4.0, offsetNorth(BENGALURU, 0.5), NOW);
    private final Driver far = available("far", CarType.HATCHBACK, 4.9, offsetNorth(BENGALURU, 1.5), NOW);

    @Test
    void nearestPutsTheCloserDriverFirst() {
        List<Driver> ranked = new NearestDriverStrategy().rank(List.of(far, near), BENGALURU);

        assertThat(ranked).extracting(Driver::id).containsExactly("near", "far");
    }

    @Test
    void highestRatedPutsTheBetterRatedDriverFirstEvenIfFurther() {
        List<Driver> ranked = new HighestRatedDriverStrategy().rank(List.of(near, far), BENGALURU);

        assertThat(ranked).extracting(Driver::id).containsExactly("far", "near");
    }

    @Test
    void highestRatedBreaksARatingTieByDistance() {
        Driver closeTie = available("close", CarType.HATCHBACK, 4.5, offsetNorth(BENGALURU, 0.4), NOW);
        Driver farTie = available("far-tie", CarType.HATCHBACK, 4.5, offsetNorth(BENGALURU, 1.8), NOW);

        List<Driver> ranked = new HighestRatedDriverStrategy().rank(List.of(farTie, closeTie), BENGALURU);

        assertThat(ranked).extracting(Driver::id).containsExactly("close", "far-tie");
    }

    @Test
    void rankingAnEmptyCandidateListYieldsAnEmptyList() {
        assertThat(new NearestDriverStrategy().rank(List.of(), BENGALURU)).isEmpty();
        assertThat(new HighestRatedDriverStrategy().rank(List.of(), BENGALURU)).isEmpty();
    }
}
