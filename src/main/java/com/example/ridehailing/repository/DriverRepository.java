package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Driver;

public interface DriverRepository extends Repository<Driver, String> {

    /**
     * Atomically claims both the driver's phone number and car plate, then stores the driver.
     * Unlike a user, a driver has two uniqueness rules, so the caller is told which one lost.
     */
    SaveResult saveIfUnique(Driver driver);

    enum SaveResult {
        SAVED,
        DUPLICATE_PHONE,
        DUPLICATE_PLATE
    }
}
