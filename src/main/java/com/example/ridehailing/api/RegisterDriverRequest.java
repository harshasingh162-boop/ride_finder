package com.example.ridehailing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDriverRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull @Valid CarRequest car
) {
}
