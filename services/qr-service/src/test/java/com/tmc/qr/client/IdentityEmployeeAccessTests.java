package com.tmc.qr.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class IdentityEmployeeAccessTests {

    @Test
    void realHttpContractChecksCurrentPermissionAndRejectsMismatchedIdentity() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID factoryId = UUID.randomUUID();
        var mapper = new ObjectMapper();
        mapper.writeValueAsString(Map.of("warmup", true));
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var responseEmployee = new java.util.concurrent.atomic.AtomicReference<>(employeeId);
        var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
        server.createContext("/oauth2/token", exchange -> {
            byte[] body = mapper.writeValueAsBytes(Map.of("access_token", "test-token", "token_type", "Bearer", "expires_in", 60));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/internal/access-checks", exchange -> {
            if (!"Bearer test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            var request = mapper.readTree(exchange.getRequestBody());
            assertEquals(employeeId.toString(), request.get("employeeId").asText());
            byte[] body = mapper.writeValueAsBytes(Map.of("code", "SUCCESS", "data", Map.of("employeeId", responseEmployee.get(),
                    "factoryId", factoryId, "allowed", allowed.get(), "reason", "CHECKED")));
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            var client = new InternalApiClient(mapper, URI.create(url + "/oauth2/token"),
                    "service-qr-test", "test-secret", "identity.verify");
            var adapter = new IdentityEmployeeAccess(client, url);
            assertTrue(adapter.allowed(employeeId, factoryId));
            allowed.set(false);
            assertFalse(adapter.allowed(employeeId, factoryId));
            responseEmployee.set(UUID.randomUUID());
            assertEquals(503, assertThrows(ResponseStatusException.class,
                    () -> adapter.allowed(employeeId, factoryId)).getStatusCode().value());
        } finally {
            server.stop(0);
        }
    }
}
