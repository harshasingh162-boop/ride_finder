package com.example.ridehailing.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Replaces the system clock so integration tests can drive offer TTLs and location staleness
 * without sleeping. Marked @Primary so every Clock injection point picks it up.
 */
@TestConfiguration
public class TestClockConfig {

    public static final Instant START = Instant.parse("2026-09-11T10:00:00Z");

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return MutableClock.startingAt(START);
    }
}
