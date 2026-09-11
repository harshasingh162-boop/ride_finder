package com.example.ridehailing.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserAndDriverApiTest {

    private static final String KIRAN = """
            {"name":"Kiran","phone":"+91-9000000002",
             "car":{"plate":"KA-01-AB-1234","model":"Swift","carType":"HATCHBACK"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private record IdHolder(@JsonProperty("id") String id) {
    }

    private String registerKiran() throws Exception {
        String body = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON).content(KIRAN))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, IdHolder.class).id();
    }

    private void pushLocation(String driverId, String json) throws Exception {
        mockMvc.perform(put("/api/v1/drivers/" + driverId + "/location")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent());
    }

    private void setOnline(String driverId, boolean online) throws Exception {
        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"online\":" + online + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void registerUserReturns201WithAGeneratedId() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha","phone":"+91-9000000000"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Asha"))
                .andExpect(jsonPath("$.phone").value("+91-9000000000"));
    }

    @Test
    void registerDriverReturns201OfflineWithTheCarEchoedBack() throws Exception {
        mockMvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON).content(KIRAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.car.plate").value("KA-01-AB-1234"))
                .andExpect(jsonPath("$.car.model").value("Swift"))
                .andExpect(jsonPath("$.car.carType").value("HATCHBACK"));
    }

    @Test
    void pushingALocationReturns204() throws Exception {
        String driverId = registerKiran();

        pushLocation(driverId, """
                {"lat":12.9716,"lng":77.5946}
                """);
    }

    @Test
    void goingOnlineAfterALocationPushReturnsAvailable() throws Exception {
        String driverId = registerKiran();
        pushLocation(driverId, """
                {"lat":12.9716,"lng":77.5946}
                """);

        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"online":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void goingOnlineTwiceIsIdempotentOverHttp() throws Exception {
        String driverId = registerKiran();
        pushLocation(driverId, """
                {"lat":12.9716,"lng":77.5946}
                """);
        setOnline(driverId, true);

        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"online":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void goingOfflineReturnsOffline() throws Exception {
        String driverId = registerKiran();
        pushLocation(driverId, """
                {"lat":12.9716,"lng":77.5946}
                """);
        setOnline(driverId, true);

        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"online":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    void duplicateUserPhoneReturns409() throws Exception {
        String asha = """
                {"name":"Asha","phone":"+91-9000000000"}
                """;
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(asha))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(asha))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void duplicateDriverPhoneReturns409() throws Exception {
        registerKiran();
        String samePhoneDifferentPlate = """
                {"name":"Meera","phone":"+91-9000000002",
                 "car":{"plate":"KA-02-CD-5678","model":"Baleno","carType":"SEDAN"}}
                """;

        mockMvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content(samePhoneDifferentPlate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void duplicateDriverPlateReturns409() throws Exception {
        registerKiran();
        String samePlateDifferentPhone = """
                {"name":"Meera","phone":"+91-9000000003",
                 "car":{"plate":"KA-01-AB-1234","model":"Baleno","carType":"SEDAN"}}
                """;

        mockMvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content(samePlateDifferentPhone))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void registerUserWithoutANameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+91-9000000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void registerUserWithABlankPhoneReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha","phone":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void registerDriverWithAnUnknownCarTypeReturns400() throws Exception {
        String suv = """
                {"name":"Meera","phone":"+91-9000000003",
                 "car":{"plate":"KA-02-CD-5678","model":"XUV","carType":"SUV"}}
                """;

        mockMvc.perform(post("/api/v1/drivers").contentType(MediaType.APPLICATION_JSON).content(suv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void pushingALocationForAnUnknownDriverReturns404() throws Exception {
        mockMvc.perform(put("/api/v1/drivers/ghost/location").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lat":12.9716,"lng":77.5946}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void pushingAnOutOfRangeLatitudeReturns400() throws Exception {
        String driverId = registerKiran();

        mockMvc.perform(put("/api/v1/drivers/" + driverId + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lat":95.0,"lng":77.5946}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void goingOnlineWithoutALocationReturns409() throws Exception {
        String driverId = registerKiran();

        mockMvc.perform(patch("/api/v1/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"online":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void changingAvailabilityForAnUnknownDriverReturns404() throws Exception {
        mockMvc.perform(patch("/api/v1/drivers/ghost/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"online":true}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
