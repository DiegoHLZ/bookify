package com.bookify.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiErrorContractIntegrationTest.FailureEndpointConfiguration.class)
class ApiErrorContractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedErrorsUseStableTraceableContract() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("X-Correlation-ID", "frontend-request-42"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "frontend-request-42"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.correlationId").value("frontend-request-42"));
    }

    @Test
    void unsafeCorrelationIdsAreReplaced() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("X-Correlation-ID", "unsafe value with spaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "X-Correlation-ID",
                        matchesPattern("[0-9a-f-]{36}")
                ))
                .andExpect(jsonPath(
                        "$.correlationId",
                        matchesPattern("[0-9a-f-]{36}")
                ));
    }

    @Test
    void malformedBearerTokensReturnUnauthorizedInsteadOfInternalErrors() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer not-a-jwt")
                        .header("X-Correlation-ID", "invalid-token-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").value("invalid-token-1"));
    }

    @Test
    void validationErrorsIncludeFieldsAndRequestContext() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Correlation-ID", "registration-1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.correlationId").value("registration-1"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @WithMockUser
    void unexpectedErrorsDoNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/test/failure")
                        .header("X-Correlation-ID", "failure-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("sensitive database detail"))));
    }

    @TestConfiguration
    static class FailureEndpointConfiguration {
        @Bean
        FailureEndpoint failureEndpoint() {
            return new FailureEndpoint();
        }
    }

    @RestController
    static class FailureEndpoint {
        @GetMapping("/api/v1/test/failure")
        void fail() {
            throw new IllegalStateException("sensitive database detail");
        }
    }
}
