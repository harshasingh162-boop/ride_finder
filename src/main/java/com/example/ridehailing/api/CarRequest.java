package com.example.ridehailing.api;

import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CarRequest(@NotBlank String plate, @NotBlank String model, @NotNull CarType carType) {

    public Car toCar() {
        return new Car(plate, model, carType);
    }
}
