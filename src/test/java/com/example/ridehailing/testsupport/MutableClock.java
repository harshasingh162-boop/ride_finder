package com.example.ridehailing.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A test-only Clock whose instant can be advanced explicitly, so tests can control
 * elapsed time (TTLs, cancellation windows, surge windows) without real sleeps.
 */
public class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = new AtomicReference<>(instant);
        this.zone = zone;
    }

    public static MutableClock startingAt(Instant initial) {
        return new MutableClock(initial, ZoneOffset.UTC);
    }

    public void advance(Duration duration) {
        instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant.get(), zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
