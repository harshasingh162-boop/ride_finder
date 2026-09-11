package com.example.ridehailing.api;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(@NotBlank String name, @NotBlank String phone) {
}
