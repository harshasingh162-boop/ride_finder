package com.example.ridehailing.config;

import com.example.ridehailing.pricing.ConditionsProvider;
import com.example.ridehailing.pricing.FareCalculator;
import com.example.ridehailing.pricing.PriceTier;
import com.example.ridehailing.pricing.PricingStrategy;
import com.example.ridehailing.pricing.SurgeStrategy;
import com.example.ridehailing.pricing.TieredPricingStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(PricingProperties.class)
public class PricingConfig {

    @Bean
    public PricingStrategy pricingStrategy(PricingProperties properties) {
        List<PriceTier> tiers = properties.tiers().stream()
                .map(tier -> new PriceTier(tier.upToKm(), tier.rate()))
                .toList();
        return new TieredPricingStrategy(tiers);
    }

    @Bean
    public FareCalculator fareCalculator(PricingStrategy pricingStrategy, List<SurgeStrategy> surgeStrategies,
                                          ConditionsProvider conditionsProvider, PricingProperties properties) {
        return new FareCalculator(pricingStrategy, surgeStrategies, conditionsProvider,
                properties.minimumFare(), properties.surgeCap());
    }
}
