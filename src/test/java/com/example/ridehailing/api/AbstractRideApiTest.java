package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.OfferStatus;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.OfferRepository;
import com.example.ridehailing.repository.RideRepository;
import com.example.ridehailing.testsupport.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared driving helpers for the ride lifecycle tests. Every helper goes through HTTP so the
 * tests exercise controllers, validation and the advice alongside the service logic.
 */
abstract class AbstractRideApiTest {

    protected static final Location PICKUP = BENGALURU;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected DriverRepository driverRepository;

    @Autowired
    protected OfferRepository offerRepository;

    @Autowired
    protected RideRepository rideRepository;

    protected String registerUser(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rider %s\",\"phone\":\"%s\"}".formatted(phone, phone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Registers a driver, pushes their location and brings them online in one step. */
    protected String onlineDriverAt(String phone, CarType carType, Location location) throws Exception {
        String body = mockMvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Driver %s","phone":"%s",
                                 "car":{"plate":"PLATE-%s","model":"Model","carType":"%s"}}
                                """.formatted(phone, phone, phone, carType)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String driverId = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/v1/drivers/" + driverId + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":%s,\"lng\":%s}".formatted(location.lat(), location.lng())))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"online\":true}"))
                .andExpect(status().isOk());
        return driverId;
    }

    protected ResultActions requestRide(String userId, Location drop, CarType carType, String couponCode)
            throws Exception {
        String coupon = couponCode == null ? "null" : "\"" + couponCode + "\"";
        return mockMvc.perform(post("/api/v1/users/" + userId + "/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"pickup":{"lat":%s,"lng":%s},"drop":{"lat":%s,"lng":%s},
                         "carType":"%s","couponCode":%s}
                        """.formatted(PICKUP.lat(), PICKUP.lng(), drop.lat(), drop.lng(), carType, coupon)));
    }

    protected String createRide(String userId, Location drop, CarType carType, String couponCode)
            throws Exception {
        String body = requestRide(userId, drop, carType, couponCode)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected ResultActions acceptFare(String userId, String rideId) throws Exception {
        return mockMvc.perform(post("/api/v1/users/%s/rides/%s/accept-fare".formatted(userId, rideId)));
    }

    protected ResultActions cancelSearch(String userId, String rideId) throws Exception {
        return mockMvc.perform(post("/api/v1/users/%s/rides/%s/cancel-search".formatted(userId, rideId)));
    }

    protected List<String> offerIdsOf(String driverId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/drivers/" + driverId + "/offers"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    protected String soleOfferOf(String driverId) throws Exception {
        List<String> offerIds = offerIdsOf(driverId);
        if (offerIds.size() != 1) {
            throw new AssertionError("expected exactly one offer for " + driverId + " but got " + offerIds);
        }
        return offerIds.getFirst();
    }

    protected ResultActions acceptOffer(String driverId, String offerId) throws Exception {
        return mockMvc.perform(post("/api/v1/drivers/%s/offers/%s/accept".formatted(driverId, offerId)));
    }

    protected ResultActions rejectOffer(String driverId, String offerId) throws Exception {
        return mockMvc.perform(post("/api/v1/drivers/%s/offers/%s/reject".formatted(driverId, offerId)));
    }

    protected ResultActions startRide(String driverId, String rideId) throws Exception {
        return mockMvc.perform(post("/api/v1/drivers/%s/rides/%s/start".formatted(driverId, rideId)));
    }

    protected ResultActions endRide(String driverId, String rideId) throws Exception {
        return mockMvc.perform(post("/api/v1/drivers/%s/rides/%s/end".formatted(driverId, rideId)));
    }

    protected ResultActions pushLocation(String driverId, Location location) throws Exception {
        return mockMvc.perform(put("/api/v1/drivers/" + driverId + "/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":%s,\"lng\":%s}".formatted(location.lat(), location.lng())));
    }

    protected ResultActions userHistory(String userId, String status) throws Exception {
        String query = status == null ? "" : "?status=" + status;
        return mockMvc.perform(get("/api/v1/users/" + userId + "/rides" + query));
    }

    protected ResultActions driverHistory(String driverId, String status) throws Exception {
        String query = status == null ? "" : "?status=" + status;
        return mockMvc.perform(get("/api/v1/drivers/" + driverId + "/rides" + query));
    }

    protected ResultActions tracking(String rideId) throws Exception {
        return mockMvc.perform(get("/api/v1/rides/" + rideId + "/tracking"));
    }

    protected String bodyOf(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    /**
     * JsonPath hands back a Double or a BigDecimal depending on how many digits the number has,
     * so numeric assertions go through this rather than guessing the boxed type.
     */
    protected double numberAt(String body, String path) {
        Number value = JsonPath.read(body, path);
        return value.doubleValue();
    }

    /** Read straight from the store: there is no read-only status endpoint, and probing via
     *  the availability endpoint would mutate the very state under assertion. */
    protected DriverStatus driverStatusOf(String driverId) {
        return driverRepository.findById(driverId).orElseThrow().status();
    }

    protected OfferStatus offerStatusOf(String offerId) {
        return offerRepository.findById(offerId).orElseThrow().status();
    }

    protected RideStatus rideStatusOf(String rideId) {
        return rideRepository.findById(rideId).orElseThrow().status();
    }
}
