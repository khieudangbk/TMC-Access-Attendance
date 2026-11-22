package com.tmc.accessevent.config;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTests {

    @Test
    void missingPolicyFailsClosed() {
        var policy = new SecurityConfig().unavailableAccessPolicy();
        var exception = assertThrows(ResponseStatusException.class,
                () -> policy.verify(UUID.randomUUID(), "device-1", "unverified-qr"));
        assertEquals(503, exception.getStatusCode().value());
    }
}
