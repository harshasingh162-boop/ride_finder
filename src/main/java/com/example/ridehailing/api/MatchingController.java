package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.matching.MatchResult;
import com.example.ridehailing.pricing.FareBreakdown;
import com.example.ridehailing.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/estimate")
    public FareBreakdown estimate(@Valid @RequestBody EstimateRequest request) {
        return matchingService.estimate(request.pickup().toLocation(), request.drop().toLocation(),
                request.carType(), request.couponCode());
    }

    @GetMapping("/nearby-drivers")
    public List<NearbyDriverResponse> nearbyDrivers(@RequestParam double lat, @RequestParam double lng,
                                                     @RequestParam CarType carType) {
        Location pickup = new Location(lat, lng);
        MatchResult result = matchingService.findNearbyDrivers(pickup, carType);
        return result.rankedCandidates().stream()
                .map(driver -> NearbyDriverResponse.from(driver, pickup))
                .toList();
    }
}
