package com.example.ridehailing.api;

import com.example.ridehailing.service.RideMatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}")
public class DriverRideController {

    private final RideMatchingService rideMatchingService;

    public DriverRideController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    @GetMapping("/offers")
    public List<OfferResponse> offers(@PathVariable String driverId) {
        return rideMatchingService.offersFor(driverId).stream().map(OfferResponse::from).toList();
    }

    @PostMapping("/offers/{offerId}/accept")
    public RideResponse acceptOffer(@PathVariable String driverId, @PathVariable String offerId) {
        return RideResponse.from(rideMatchingService.acceptOffer(driverId, offerId));
    }

    @PostMapping("/offers/{offerId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectOffer(@PathVariable String driverId, @PathVariable String offerId) {
        rideMatchingService.rejectOffer(driverId, offerId);
    }

    @PostMapping("/rides/{rideId}/start")
    public RideResponse startRide(@PathVariable String driverId, @PathVariable String rideId) {
        return RideResponse.from(rideMatchingService.startRide(driverId, rideId));
    }

    @PostMapping("/rides/{rideId}/end")
    public RideResponse endRide(@PathVariable String driverId, @PathVariable String rideId) {
        return RideResponse.from(rideMatchingService.endRide(driverId, rideId));
    }
}
