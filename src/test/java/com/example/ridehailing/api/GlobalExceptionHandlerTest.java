package com.example.ridehailing.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExceptionTestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("ride not found"));
    }

    @Test
    void invalidStateExceptionMapsTo409() throws Exception {
        mockMvc.perform(post("/test/invalid-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("ride is not in a cancellable state"));
    }

    @Test
    void forbiddenExceptionMapsTo403() throws Exception {
        mockMvc.perform(post("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("not your ride"));
    }

    @Test
    void duplicateExceptionMapsTo409() throws Exception {
        mockMvc.perform(post("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("phone already registered"));
    }

    @Test
    void invalidCouponExceptionMapsTo400() throws Exception {
        mockMvc.perform(post("/test/invalid-coupon"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("coupon has expired"));
    }

    @Test
    void beanValidationFailureMapsTo400() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name must not be blank"));
    }

    @Test
    void successfulRequestIsUnaffectedByTheAdvice() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Asha\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
