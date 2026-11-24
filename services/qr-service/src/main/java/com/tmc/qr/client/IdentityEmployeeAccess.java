package com.tmc.qr.client;

import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.qr.dto.request.AccessRequestDTO;
import com.tmc.qr.dto.response.AccessResultDTO;

@Component
@ConditionalOnProperty(name = "tmc.internal.enabled", havingValue = "true")
public class IdentityEmployeeAccess implements EmployeeAccess {

    private final InternalApiClient client;
    private final URI checkUri;

    public IdentityEmployeeAccess(InternalApiClient client,
            @Value("${tmc.internal.identity-url:http://localhost:8081}") String identityUrl) {
        this.client = client;
        this.checkUri = URI.create(identityUrl + "/internal/access-checks");
    }

    @Override
    public boolean allowed(UUID employeeId, UUID factoryId) {
        var result = client.post(checkUri, new AccessRequestDTO(employeeId, factoryId), AccessResultDTO.class);
        if (!employeeId.equals(result.employeeId()) || !factoryId.equals(result.factoryId()) || result.allowed() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVALID_IDENTITY_RESPONSE");
        }
        return result.allowed();
    }
}
