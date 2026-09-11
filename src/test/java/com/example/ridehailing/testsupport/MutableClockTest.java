package com.example.ridehailing.testsupport;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MutableClockTest {

    @Test
    void advanceMovesTheInstantForwardByTheGivenDuration() {
        Instant start = Instant.parse("2026-09-11T10:00:00Z");
        MutableClock clock = MutableClock.startingAt(start);

        clock.advance(Duration.ofMinutes(5));

        assertThat(clock.instant()).isEqualTo(start.plus(Duration.ofMinutes(5)));
    }
}
