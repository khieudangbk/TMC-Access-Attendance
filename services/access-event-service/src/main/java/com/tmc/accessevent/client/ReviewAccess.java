package com.tmc.accessevent.client;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import com.tmc.accessevent.dto.response.AuthorizationCheckDTO;
import com.tmc.accessevent.dto.request.AuthorizationRequestDTO;
import com.tmc.api.ServiceApiClient;

@Component
public class ReviewAccess {
    private final ServiceApiClient client;
    private final String identityUrl;

    public ReviewAccess(ObjectMapper json,
            @Value("${tmc.internal.token-uri:http://localhost:8081/oauth2/token}") URI tokenUri,
            @Value("${tmc.internal.client-id:}") String clientId,
            @Value("${tmc.internal.client-secret:}") String clientSecret,
            @Value("${tmc.internal.identity-url:http://localhost:8081}") String identityUrl) {
        client = new ServiceApiClient(json, tokenUri, clientId, clientSecret, "identity.verify");
        this.identityUrl = identityUrl;
    }

    public UUID require(Jwt jwt, UUID factoryId, Set<String> roles) {
        UUID employeeId = employee(jwt);
        var request = new AuthorizationRequestDTO(employeeId, factoryId, roles);
        var check = client.post(URI.create(identityUrl + "/internal/authorization-checks"), request, AuthorizationCheckDTO.class);
        if (check == null || !employeeId.equals(check.employeeId()) || !factoryId.equals(check.factoryId())
                || check.allowed() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVALID_AUTHORIZATION_RESPONSE");
        }
        if (!check.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FACTORY_ROLE_REQUIRED");
        }
        return employeeId;
    }

    public UUID employee(Jwt jwt) {
        try {
            UUID employeeId = UUID.fromString(jwt.getSubject());
            if (!employeeId.toString().equals(jwt.getClaimAsString("employee_id"))) {
                throw new IllegalArgumentException();
            }
            return employeeId;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMPLOYEE_IDENTITY_REQUIRED");
        }
    }
}
