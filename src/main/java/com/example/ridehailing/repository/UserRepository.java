package com.example.ridehailing.repository;

import com.example.ridehailing.domain.User;

public interface UserRepository extends Repository<User, String> {

    /**
     * Atomically claims the user's phone number and stores the user.
     *
     * @return false if the phone number was already taken, in which case nothing is stored
     */
    boolean saveIfPhoneAbsent(User user);
}
