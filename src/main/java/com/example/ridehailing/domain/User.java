package com.example.ridehailing.domain;

import java.util.Objects;

public record User(String id, String name, String phone) {

    public User {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(phone, "phone must not be null");
    }
}
