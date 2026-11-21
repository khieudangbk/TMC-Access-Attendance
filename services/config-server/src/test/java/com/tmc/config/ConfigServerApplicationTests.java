package com.tmc.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.security.user.name=config-test",
        "spring.security.user.password=config-test-password"
})
class ConfigServerApplicationTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void configurationRequiresAuthentication() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/identity-user-service/default")).GET().build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    void servesSharedAndApplicationConfigurationForAllClients() throws Exception {
        var credentials = Base64.getEncoder().encodeToString(
                "config-test:config-test-password".getBytes(StandardCharsets.UTF_8));
        try (var client = HttpClient.newHttpClient()) {
            for (String service : new String[]{"discovery-server", "api-gateway",
                    "identity-user-service", "qr-service", "gate-station-service",
                    "access-event-service"}) {
                var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/" + service + "/default"))
                        .header("Authorization", "Basic " + credentials).GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), service);
                assertTrue(response.body().contains(service + ".yaml"), service);
                assertTrue(response.body().contains("configurations/application.yaml"), service);
            }
        }
    }


    @Test
    void gatewayOnlyRoutesToTheFourRemainingBusinessServices() throws Exception {
        var credentials = Base64.getEncoder().encodeToString(
                "config-test:config-test-password".getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api-gateway/default"))
                .header("Authorization", "Basic " + credentials).GET().build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            for (String service : new String[]{"identity-user-service", "qr-service",
                    "gate-station-service", "access-event-service"}) {
                assertTrue(response.body().contains("lb://" + service), service);
            }
            for (String service : new String[]{"attendance-report-service", "hr-integration-service",
                    "audit-notification-service"}) {
                assertFalse(response.body().contains("lb://" + service), service);
            }
        }
    }
}
