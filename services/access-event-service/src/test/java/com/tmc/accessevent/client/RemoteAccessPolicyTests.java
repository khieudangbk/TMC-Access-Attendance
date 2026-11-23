package com.tmc.accessevent.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class RemoteAccessPolicyTests {

    private final UUID stationId = UUID.randomUUID();
    private final UUID factoryId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID qrId = UUID.randomUUID();
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final AtomicInteger permissionRequests = new AtomicInteger();
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;
    private String baseUrl;
    private boolean deviceActive = true;
    private boolean qrValid = true;
    private boolean permissionAllowed = true;
    private boolean stationMismatch;
    private int delayMillis;
    private String stationResponseOverride;
    private RemoteAccessPolicy policy;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                Object body;
                if (path.equals("/oauth2/token")) {
                    tokenRequests.incrementAndGet();
                    String form = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!form.contains("client_credentials") || !form.contains("identity.verify")) {
                        exchange.sendResponseHeaders(400, -1);
                        return;
                    }
                    body = Map.of("access_token", "test-service-token", "token_type", "Bearer", "expires_in", 60);
                } else {
                    if (!"Bearer test-service-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                    if (path.startsWith("/internal/devices/")) {
                        body = Map.of("clientId", "kiosk-test", "stationId", stationId, "active", deviceActive);
                    } else if (path.startsWith("/internal/stations/")) {
                        body = Map.of("stationId", stationMismatch ? UUID.randomUUID() : stationId,
                                "factoryId", factoryId, "direction", "ENTRY", "allowed", true,
                                "reason", "ALLOWED", "revision", 1);
                    } else if (path.equals("/internal/qr/verifications")) {
                        body = Map.of("valid", qrValid, "reason", "VALID", "qrId", qrId,
                                "employeeId", employeeId, "factoryId", factoryId,
                                "expiresAt", Instant.now().plusSeconds(60).toString());
                    } else {
                        permissionRequests.incrementAndGet();
                        body = Map.of("employeeId", employeeId, "factoryId", factoryId,
                                "allowed", permissionAllowed, "reason", "ALLOWED");
                    }
                }
                byte[] response = path.startsWith("/internal/stations/") && stationResponseOverride != null
                        ? stationResponseOverride.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        : mapper.writeValueAsBytes(path.equals("/oauth2/token") ? body : Map.of("code", "SUCCESS", "data", body));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        var client = new InternalApiClient(mapper, java.net.URI.create(baseUrl + "/oauth2/token"),
                "service-access-test", "test-secret-only", "identity.verify qr.verify stations.verify");
        policy = new RemoteAccessPolicy(client, baseUrl, baseUrl, baseUrl);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void validPolicyUsesAllContractsAndCachesServiceToken() {
        var first = policy.verify(stationId, "kiosk-test", "signed-qr");
        assertTrue(first.allowed());
        assertEquals(employeeId, first.employeeId());
        assertEquals(qrId, first.qrId());
        assertEquals("ENTRY", first.direction());
        assertTrue(policy.verify(stationId, "kiosk-test", "signed-qr").allowed());
        assertEquals(1, tokenRequests.get());
        assertEquals(2, permissionRequests.get());
    }

    @Test
    void disabledDeviceIsCheckedEvenWhenJwtWasIssuedEarlier() {
        deviceActive = false;
        assertFalse(policy.verify(stationId, "kiosk-test", "signed-qr").allowed());
        assertEquals(0, permissionRequests.get());
    }

    @Test
    void revokedQrAndCurrentFactoryPermissionDenyAuthorization() {
        qrValid = false;
        assertFalse(policy.verify(stationId, "kiosk-test", "signed-qr").allowed());
        assertEquals(0, permissionRequests.get());
        qrValid = true;
        permissionAllowed = false;
        assertEquals("SITE_ACCESS_DENIED", policy.verify(stationId, "kiosk-test", "signed-qr").reason());
    }

    @Test
    void mismatchedDependencyResponseFailsClosed() {
        stationMismatch = true;
        var exception = assertThrows(ResponseStatusException.class,
                () -> policy.verify(stationId, "kiosk-test", "signed-qr"));
        assertEquals(503, exception.getStatusCode().value());
        assertEquals(0, permissionRequests.get());
    }

    @Test
    void slowDependencyHasBoundedWaitAndDoesNotGrantAccess() {
        delayMillis = 600;
        long start = System.nanoTime();
        var exception = assertThrows(ResponseStatusException.class,
                () -> policy.verify(stationId, "kiosk-test", "signed-qr"));
        assertEquals(503, exception.getStatusCode().value());
        assertTrue(java.time.Duration.ofNanos(System.nanoTime() - start).compareTo(java.time.Duration.ofSeconds(2)) < 0);
        assertEquals(0, permissionRequests.get());
    }

    @Test
    void nullMissingFieldsAndWrongTypesAreSafeDependencyErrors() {
        for (String malformed : new String[]{"null", "{}", "{\"allowed\":\"not-a-boolean\"}", "[]"}) {
            stationResponseOverride = malformed;
            var exception = assertThrows(ResponseStatusException.class,
                    () -> policy.verify(stationId, "kiosk-test", "signed-qr"));
            assertEquals(503, exception.getStatusCode().value());
        }
        assertEquals(0, permissionRequests.get());
    }
}
