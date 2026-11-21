package com.tmc.api;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApiResponseTests.Application.class)
@AutoConfigureMockMvc
class ApiResponseTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void successHasOneEnvelopeAndSafeCorrelationIdentifier() throws Exception {
        mockMvc.perform(get("/fixture/value").header("X-Correlation-ID", "test-request-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.value").value("ready"))
                .andExpect(jsonPath("$.traceId").value("test-request-1"))
                .andExpect(header().string("X-Correlation-ID", "test-request-1"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty()).andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void headAndNoContentDoNotHaveJsonBodies() throws Exception {
        mockMvc.perform(head("/fixture/value")).andExpect(status().isOk()).andExpect(content().string(""));
        mockMvc.perform(delete("/fixture/value")).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void validationAndMalformedJsonDoNotEchoRejectedSecrets() throws Exception {
        mockMvc.perform(post("/fixture/value").contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name")).andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(post("/fixture/value").contentType("application/json").content("{private-secret"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-secret"))));
    }

    @Test
    void errorsPreserveStatusAndHideInternalDetails() throws Exception {
        mockMvc.perform(get("/fixture/unavailable")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
        mockMvc.perform(get("/fixture/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-database-password"))));
        mockMvc.perform(get("/fixture/number/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/fixture/secure")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void oauthAndJwksKeepProtocolPayloads() throws Exception {
        mockMvc.perform(get("/oauth2/test-response")).andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer")).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get("/internal/qr/jwks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray()).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void missingConflictingForbiddenAndEmptyRequestsKeepTheirContracts() throws Exception {
        mockMvc.perform(get("/fixture/status/404")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/fixture/status/409")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        mockMvc.perform(get("/fixture/forbidden")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(post("/fixture/value").contentType("application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_JSON"));
        mockMvc.perform(post("/fixture/value").contentType("text/plain").content("input"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        FixtureController fixtureController() {
            return new FixtureController();
        }

        @Bean
        SecurityFilterChain security(HttpSecurity http, ApiSecurityHandlers handlers) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.requestMatchers("/fixture/secure").authenticated()
                            .anyRequest().permitAll())
                    .exceptionHandling(errors -> errors.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                    .build();
        }
    }

    @RestController
    static class FixtureController {
        @GetMapping("/fixture/value")
        Map<String, String> value() {
            return Map.of("value", "ready");
        }

        @DeleteMapping("/fixture/value")
        ResponseEntity<Void> delete() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/fixture/value")
        Payload create(@Valid @RequestBody Payload payload) {
            return payload;
        }

        @GetMapping("/fixture/unavailable")
        void unavailable() {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE");
        }

        @GetMapping("/fixture/failure")
        void failure() {
            throw new IllegalStateException("private-database-password");
        }

        @GetMapping("/fixture/number/{number}")
        Map<String, Long> number(@PathVariable Long number) {
            return Map.of("number", number);
        }

        @GetMapping("/fixture/status/{status}")
        void status(@PathVariable int status) {
            throw new ResponseStatusException(HttpStatus.valueOf(status), status == 404 ? "RESOURCE_NOT_FOUND" : "IDEMPOTENCY_CONFLICT");
        }

        @GetMapping("/fixture/forbidden")
        void forbidden() {
            throw new org.springframework.security.access.AccessDeniedException("private policy details");
        }

        @GetMapping("/oauth2/test-response")
        Map<String, String> token() {
            return Map.of("token_type", "Bearer");
        }

        @GetMapping("/internal/qr/jwks")
        Map<String, Object> keys() {
            return Map.of("keys", java.util.List.of());
        }

        record Payload(@NotBlank String name) {
        }
    }
}
