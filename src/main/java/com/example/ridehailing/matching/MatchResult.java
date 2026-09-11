package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;

import java.util.List;

/**
 * @param assignedCarType the car type the candidates actually drive, which differs from the
 *                        requested type only when {@code upgraded} is true
 */
public record MatchResult(List<Driver> rankedCandidates, CarType assignedCarType, boolean upgraded) {

    public boolean isEmpty() {
        return rankedCandidates.isEmpty();
    }
}
