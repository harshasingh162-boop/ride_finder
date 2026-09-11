package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDriverRepositoryTest {

    private final InMemoryDriverRepository repository = new InMemoryDriverRepository();
    private final Car car = new Car("KA-01-AB-1234", "Swift", CarType.HATCHBACK);

    @Test
    void savedDriverCanBeFoundById() {
        Driver driver = new Driver("driver-1", "Kiran", "+91-9000000002", car);

        repository.save(driver);

        assertThat(repository.findById("driver-1")).contains(driver);
    }

    @Test
    void findAllReturnsEverySavedDriver() {
        Driver first = new Driver("driver-1", "Kiran", "+91-9000000002", car);
        Driver second = new Driver("driver-2", "Meera", "+91-9000000003", car);

        repository.save(first);
        repository.save(second);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById("missing")).isEmpty();
    }
}
