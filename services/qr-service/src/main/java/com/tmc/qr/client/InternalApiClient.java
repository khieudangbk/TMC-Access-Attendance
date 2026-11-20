package com.tmc.qr.client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "tmc.internal.enabled", havingValue = "true")
public class InternalApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(250);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(150))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper objectMapper;
    private final URI tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scopes;
    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public InternalApiClient(ObjectMapper objectMapper,
            @Value("${tmc.internal.token-uri:http://localhost:8081/oauth2/token}") URI tokenUri,
            @Value("${tmc.internal.client-id}") String clientId,
            @Value("${tmc.internal.client-secret:}") String clientSecret,
            @Value("${tmc.internal.scopes}") String scopes) {
        this.objectMapper = objectMapper;
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    public <T> T get(URI uri, Class<T> resultType) {
        return send(uri, null, resultType);
    }

    public <T> T post(URI uri, Object request, Class<T> resultType) {
        return send(uri, request, resultType);
    }

    private <T> T send(URI uri, Object request, Class<T> resultType) {
        try {
            var builder = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken()).header("Accept", "application/json");
            if (request == null) {
                builder.GET();
            } else {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)));
            }
            var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw unavailable();
            }
            var envelope = objectMapper.readTree(response.body());
            if (!"SUCCESS".equals(envelope.path("code").asText()) || envelope.path("data").isMissingNode()
                    || envelope.path("data").isNull()) {
                throw unavailable();
            }
            T result = objectMapper.treeToValue(envelope.get("data"), resultType);
            if (result == null) {
                throw unavailable();
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (java.io.IOException | tools.jackson.core.JacksonException exception) {
            throw unavailable();
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && tokenExpiresAt.isAfter(Instant.now().plusSeconds(5))) {
            return cachedToken;
        }
        if (clientSecret.isBlank() || clientId.isBlank()) {
            throw unavailable();
        }
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String form = "grant_type=client_credentials&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8);
        try {
            var request = HttpRequest.newBuilder(tokenUri).timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Basic " + credentials).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw unavailable();
            }
            var token = objectMapper.readTree(response.body());
            String value = token.path("access_token").asText();
            int expiresIn = token.path("expires_in").asInt();
            if (value.isBlank() || expiresIn <= 5 || !"Bearer".equalsIgnoreCase(token.path("token_type").asText())) {
                throw unavailable();
            }
            cachedToken = value;
            tokenExpiresAt = Instant.now().plusSeconds(Math.min(expiresIn, 120));
            return value;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (java.io.IOException | tools.jackson.core.JacksonException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INTERNAL_DEPENDENCY_UNAVAILABLE");
    }
}
