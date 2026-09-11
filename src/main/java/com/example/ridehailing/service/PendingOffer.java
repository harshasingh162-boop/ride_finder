package com.example.ridehailing.service;

import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideOffer;

/** An offer paired with the ride it is for, so the API can show pickup, drop and fare together. */
public record PendingOffer(RideOffer offer, Ride ride) {
}
