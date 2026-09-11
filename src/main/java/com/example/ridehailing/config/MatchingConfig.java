package com.example.ridehailing.config;

import com.example.ridehailing.matching.DriverMatchingStrategy;
import com.example.ridehailing.matching.HighestRatedDriverStrategy;
import com.example.ridehailing.matching.LinearScanProximitySearch;
import com.example.ridehailing.matching.NearestDriverStrategy;
import com.example.ridehailing.matching.ProximitySearch;
import com.example.ridehailing.repository.DriverRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
public class MatchingConfig {

    @Bean
    public ProximitySearch proximitySearch(DriverRepository driverRepository, Clock clock,
                                            MatchingProperties properties) {
        return new LinearScanProximitySearch(driverRepository, clock,
                properties.radiusKm(), properties.locationStaleness());
    }

    /**
     * One switch in one place decides the ranking strategy, so booking code just injects
     * DriverMatchingStrategy and never learns which implementation it got. Adding a strategy
     * to the enum breaks this switch at compile time, which is the reminder we want.
     */
    @Bean
    public DriverMatchingStrategy driverMatchingStrategy(MatchingProperties properties) {
        return switch (properties.strategy()) {
            case NEAREST -> new NearestDriverStrategy();
            case HIGHEST_RATED -> new HighestRatedDriverStrategy();
        };
    }
}
