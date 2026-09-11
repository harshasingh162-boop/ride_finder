package com.example.ridehailing.api;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack HTTP tests. The context is rebuilt per test because the in-memory coupon
 * store and the ConditionsProvider are application-scoped singletons.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponAndEstimateApiTest {

    private static final String SAVE20 = """
            {"code":"SAVE20","type":"PERCENTAGE","percent":20,"maxDiscount":30,"validUntil":"2030-01-01T00:00:00Z"}
            """;

    /**
     * ~111 m apart, so the tiered fare is far below the 50 minimum. The minimum fare
     * therefore pins the subtotal at exactly 50 and the expected totals stay exact
     * regardless of the haversine decimals.
     */
    private static final String SHORT_TRIP_HATCHBACK = """
            {"pickup":{"lat":12.9716,"lng":77.5946},"drop":{"lat":12.9726,"lng":77.5946},"carType":"HATCHBACK"}
            """;

    @Autowired
    private MockMvc mockMvc;

    private void createSave20() throws Exception {
        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON).content(SAVE20))
                .andExpect(status().isCreated());
    }

    @Test
    void createCouponReturns201WithTheStoredCoupon() throws Exception {
        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON).content(SAVE20))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCouponReturns409ForADuplicateCode() throws Exception {
        createSave20();

        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON).content(SAVE20))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void createCouponReturns400WhenPercentIsOutOfRange() throws Exception {
        String badPercent = """
                {"code":"NOPE","type":"PERCENTAGE","percent":101,"maxDiscount":30,"validUntil":"2030-01-01T00:00:00Z"}
                """;

        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON).content(badPercent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void deleteCouponReturns204() throws Exception {
        createSave20();

        mockMvc.perform(delete("/api/v1/admin/coupons/SAVE20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUnknownCouponReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/coupons/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void listShowsDeletedCouponsWithTheirActiveFlagCleared() throws Exception {
        createSave20();
        String half50 = """
                {"code":"HALF50","type":"PERCENTAGE","percent":50,"maxDiscount":30,"validUntil":"2030-01-01T00:00:00Z"}
                """;
        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON).content(half50))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/admin/coupons/HALF50")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.code == 'SAVE20')].active").value(true))
                .andExpect(jsonPath("$[?(@.code == 'HALF50')].active").value(false));
    }

    @Test
    void estimateAppliesAValidCoupon() throws Exception {
        createSave20();
        String withCoupon = """
                {"pickup":{"lat":12.9716,"lng":77.5946},"drop":{"lat":12.9726,"lng":77.5946},
                 "carType":"HATCHBACK","couponCode":"save20"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON).content(withCoupon))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"carType":"HATCHBACK","subtotal":50,"minimumFareApplied":true,
                         "surgeMultiplier":1,"discount":10,"total":40.00}
                        """));
    }

    @Test
    void estimateWithoutACouponChargesTheMinimumFare() throws Exception {
        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content(SHORT_TRIP_HATCHBACK))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"minimumFareApplied":true,"discount":0,"total":50.00}
                        """));
    }

    @Test
    void estimateOverALongTripPricesOnDistanceRatherThanTheMinimum() throws Exception {
        String longTrip = """
                {"pickup":{"lat":0.0,"lng":0.0},"drop":{"lat":1.0,"lng":0.0},"carType":"HATCHBACK"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON).content(longTrip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimumFareApplied").value(false))
                .andExpect(jsonPath("$.distanceKm").value(Matchers.closeTo(111.19, 0.01)));
    }

    @Test
    void estimateReturns400ForAnUnknownCoupon() throws Exception {
        String unknownCoupon = """
                {"pickup":{"lat":12.9716,"lng":77.5946},"drop":{"lat":12.9726,"lng":77.5946},
                 "carType":"HATCHBACK","couponCode":"GHOST"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content(unknownCoupon))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void estimateReturns400WhenDropIsMissing() throws Exception {
        String noDrop = """
                {"pickup":{"lat":12.9716,"lng":77.5946},"carType":"HATCHBACK"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON).content(noDrop))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void estimateReturns400ForAnOutOfRangeLatitude() throws Exception {
        String badLat = """
                {"pickup":{"lat":200.0,"lng":77.5946},"drop":{"lat":12.9726,"lng":77.5946},"carType":"HATCHBACK"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON).content(badLat))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void estimateReturns400WhenPickupEqualsDrop() throws Exception {
        String samePoint = """
                {"pickup":{"lat":12.9716,"lng":77.5946},"drop":{"lat":12.9716,"lng":77.5946},"carType":"HATCHBACK"}
                """;

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON).content(samePoint))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void settingRainyConditionsSurgesTheNextEstimate() throws Exception {
        mockMvc.perform(put("/api/v1/admin/conditions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"raining":true,"trafficLevel":"LOW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raining").value(true));

        mockMvc.perform(post("/api/v1/matching/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content(SHORT_TRIP_HATCHBACK))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"surgeMultiplier":1.20,"total":60.00}
                        """));
    }

    @Test
    void updatingConditionsReturns400ForAnUnknownTrafficLevel() throws Exception {
        mockMvc.perform(put("/api/v1/admin/conditions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"raining":true,"trafficLevel":"GRIDLOCK"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
