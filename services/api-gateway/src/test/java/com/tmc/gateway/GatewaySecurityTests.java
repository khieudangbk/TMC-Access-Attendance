package com.tmc.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=classpath:gateway-security-test.yaml"
})
class GatewaySecurityTests {

    @Value("${local.server.port}")
    private int port;

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    @Test
    void businessApiRequiresAuthentication() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request("/api/access/scans").GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(401, response.statusCode());
        }
    }

    @Test
    void oauthTokenRequestReachesRoutingWithoutBearerOrCsrfToken() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request("/api/identity/oauth2/token")
                    .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            // Isolated context has no proxy routes: 404 proves security did not block it.
            assertEquals(404, response.statusCode());
        }
    }

    @Test
    void allowedFrontendCanSendAuthenticatedApiPreflight() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request("/api/access/scans")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "authorization,content-type")
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(200, response.statusCode());
            assertEquals("http://localhost:3000",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        }
    }

    @Test
    void unlistedOriginCannotSendApiPreflight() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request("/api/access/scans")
                    .header("Origin", "https://unlisted.example")
                    .header("Access-Control-Request-Method", "POST")
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(403, response.statusCode());
        }
    }
}
