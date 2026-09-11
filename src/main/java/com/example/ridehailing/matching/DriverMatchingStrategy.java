package com.example.ridehailing.matching;

import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;

import java.util.List;

/** Orders proximity-search candidates into the sequence offers should be made in. */
public interface DriverMatchingStrategy {

    List<Driver> rank(List<Driver> candidates, Location pickup);
}
