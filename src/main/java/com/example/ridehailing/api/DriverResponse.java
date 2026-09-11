package com.example.ridehailing.api;

import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;

public record DriverResponse(String id, String name, String phone, CarResponse car,
                             DriverStatus status, double rating) {

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(driver.id(), driver.name(), driver.phone(),
                CarResponse.from(driver.car()), driver.status(), driver.rating());
    }

    public record CarResponse(String plate, String model, CarType carType) {

        static CarResponse from(Car car) {
            return new CarResponse(car.plate(), car.model(), car.type());
        }
    }
}
