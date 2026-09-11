package com.example.ridehailing.api;

import com.example.ridehailing.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse register(@Valid @RequestBody RegisterDriverRequest request) {
        return DriverResponse.from(
                driverService.register(request.name(), request.phone(), request.car().toCar()));
    }

    @PutMapping("/{driverId}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@PathVariable String driverId, @Valid @RequestBody LocationRequest request) {
        driverService.updateLocation(driverId, request.toLocation());
    }

    @PatchMapping("/{driverId}/availability")
    public DriverStatusResponse updateAvailability(@PathVariable String driverId,
                                                    @Valid @RequestBody UpdateAvailabilityRequest request) {
        return new DriverStatusResponse(driverService.setAvailability(driverId, request.online()));
    }
}
