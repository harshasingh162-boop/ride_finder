package com.example.ridehailing.config;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.matching.DriverMatchingStrategy;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.InMemoryDriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static org.assertj.core.api.Assertions.assertThat;

class MatchingConfigTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    /** Closer but rated lower, so the two strategies must order these oppositely. */
    private static final Driver NEAR_AND_WORSE =
            available("near", CarType.HATCHBACK, 4.0, offsetNorth(BENGALURU, 0.5), NOW);
    private static final Driver FAR_AND_BETTER =
            available("far", CarType.HATCHBACK, 4.9, offsetNorth(BENGALURU, 1.5), NOW);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MatchingConfig.class)
            .withBean(DriverRepository.class, InMemoryDriverRepository::new)
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues(
                    "ride-hailing.matching.radius-km=2.0",
                    "ride-hailing.matching.location-staleness=60s");

    private static List<String> rankedIdsFrom(DriverMatchingStrategy strategy) {
        return strategy.rank(List.of(NEAR_AND_WORSE, FAR_AND_BETTER), BENGALURU)
                .stream().map(Driver::id).toList();
    }

    @Test
    void theNearestStrategyIsWiredWhenThePropertySaysNearest() {
        contextRunner.withPropertyValues("ride-hailing.matching.strategy=nearest")
                .run(context -> assertThat(rankedIdsFrom(context.getBean(DriverMatchingStrategy.class)))
                        .containsExactly("near", "far"));
    }

    @Test
    void theHighestRatedStrategyIsWiredWhenThePropertySaysHighestRated() {
        contextRunner.withPropertyValues("ride-hailing.matching.strategy=highest-rated")
                .run(context -> assertThat(rankedIdsFrom(context.getBean(DriverMatchingStrategy.class)))
                        .containsExactly("far", "near"));
    }

    @Test
    void contextFailsToStartForAnUnknownStrategyName() {
        contextRunner.withPropertyValues("ride-hailing.matching.strategy=telepathic")
                .run(context -> assertThat(context).hasFailed());
    }
}
