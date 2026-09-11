package com.example.ridehailing.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CarTypeTest {

    @Test
    void hatchbackFreeUpgradesToSedan() {
        assertThat(CarType.HATCHBACK.freeUpgrade()).isEqualTo(Optional.of(CarType.SEDAN));
    }

    @Test
    void sedanHasNoFreeUpgrade() {
        assertThat(CarType.SEDAN.freeUpgrade()).isEmpty();
    }
}
