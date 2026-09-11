package com.example.ridehailing.api;

import com.example.ridehailing.domain.Location;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(@NotNull Double lat, @NotNull Double lng) {

    /** Range checking lives in Location itself, so the bounds are stated in exactly one place. */
    public Location toLocation() {
        return new Location(lat, lng);
    }
}
