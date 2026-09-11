package com.example.ridehailing.domain;

import java.util.Objects;

public record Car(String plate, String model, CarType type) {

    public Car {
        Objects.requireNonNull(plate, "plate must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
