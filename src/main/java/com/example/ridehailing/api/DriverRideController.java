package com.example.ridehailing.api;

import com.example.ridehailing.domain.RideHistoryFilter;
import com.example.ridehailing.service.RideHistoryService;
import com.example.ridehailing.service.RideMatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}")
public class DriverRideController {

    private final RideMatchingService rideMatchingService;
    private final RideHistoryService rideHistoryService;

    public DriverRideController(RideMatchingService rideMatchingService, RideHistoryService rideHistoryService) {
        this.rideMatchingService = rideMatchingService;
        this.rideHistoryService = rideHistoryService;
    }

    @GetMapping("/rides")
    public List<RideResponse> history(@PathVariable String driverId,
                                       @RequestParam(required = false) RideHistoryFilter status) {
        return rideHistoryService.forDriver(driverId, Optional.ofNullable(status)).stream()
                .map(RideResponse::from).toList();
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
