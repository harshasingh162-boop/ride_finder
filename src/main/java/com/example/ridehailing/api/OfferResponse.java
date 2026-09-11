package com.example.ridehailing.api;

import com.example.ridehailing.pricing.FareBreakdown;
import com.example.ridehailing.service.PendingOffer;

import java.time.Instant;

public record OfferResponse(String id, String rideId, LocationResponse pickup, LocationResponse drop,
                            FareBreakdown fare, Instant expiresAt) {

    public static OfferResponse from(PendingOffer pendingOffer) {
        return new OfferResponse(
                pendingOffer.offer().id(),
                pendingOffer.ride().id(),
                LocationResponse.from(pendingOffer.ride().pickup()),
                LocationResponse.from(pendingOffer.ride().drop()),
                pendingOffer.ride().quotedFare(),
                pendingOffer.offer().expiresAt());
    }
}
