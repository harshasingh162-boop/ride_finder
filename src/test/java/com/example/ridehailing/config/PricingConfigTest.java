package com.example.ridehailing.config;

import com.example.ridehailing.pricing.ConditionsProvider;
import com.example.ridehailing.pricing.RainSurgeStrategy;
import com.example.ridehailing.pricing.TrafficSurgeStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PricingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PricingConfig.class, ConditionsProvider.class,
                    RainSurgeStrategy.class, TrafficSurgeStrategy.class);

    @Test
    void contextFailsToStartWhenTiersOverlap() {
        contextRunner
                .withPropertyValues(
                        "ride-hailing.pricing.minimum-fare=50",
                        "ride-hailing.pricing.surge-cap=2.0",
                        "ride-hailing.pricing.tiers[0].up-to-km=5",
                        "ride-hailing.pricing.tiers[0].rate=10",
                        "ride-hailing.pricing.tiers[1].up-to-km=2",
                        "ride-hailing.pricing.tiers[1].rate=8",
                        "ride-hailing.pricing.tiers[2].rate=5"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextStartsSuccessfullyWithValidTiers() {
        contextRunner
                .withPropertyValues(
                        "ride-hailing.pricing.minimum-fare=50",
                        "ride-hailing.pricing.surge-cap=2.0",
                        "ride-hailing.pricing.tiers[0].up-to-km=2",
                        "ride-hailing.pricing.tiers[0].rate=10",
                        "ride-hailing.pricing.tiers[1].up-to-km=5",
                        "ride-hailing.pricing.tiers[1].rate=8",
                        "ride-hailing.pricing.tiers[2].rate=5"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }
}
