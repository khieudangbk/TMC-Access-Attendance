package com.tmc.accessevent.client;

import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.dto.response.DeviceResultDTO;
import com.tmc.accessevent.dto.request.PermissionRequestDTO;
import com.tmc.accessevent.dto.response.PermissionResultDTO;
import com.tmc.accessevent.dto.request.QrRequestDTO;
import com.tmc.accessevent.dto.response.QrResultDTO;
import com.tmc.accessevent.dto.request.StationRequestDTO;
import com.tmc.accessevent.dto.response.StationResultDTO;
import com.tmc.accessevent.entity.VerifiedAccess;
import com.tmc.accessevent.service.AccessPolicy;

@Component
@ConditionalOnProperty(name = "tmc.internal.enabled", havingValue = "true")
public class RemoteAccessPolicy implements AccessPolicy {

    private final InternalApiClient client;
    private final String identityUrl;
    private final String stationUrl;
    private final String qrUrl;

    public RemoteAccessPolicy(InternalApiClient client,
            @Value("${tmc.internal.identity-url:http://localhost:8081}") String identityUrl,
            @Value("${tmc.internal.station-url:http://localhost:8083}") String stationUrl,
            @Value("${tmc.internal.qr-url:http://localhost:8082}") String qrUrl) {
        this.client = client;
        this.identityUrl = identityUrl;
        this.stationUrl = stationUrl;
        this.qrUrl = qrUrl;
    }

    @Override
    public VerifiedAccess verify(UUID stationId, String deviceId, String qrToken) {
        if (!deviceId.matches("kiosk-[a-zA-Z0-9-]{1,90}")) {
            return denied("INVALID_DEVICE_ID");
        }
        var device = client.get(URI.create(identityUrl + "/internal/devices/" + deviceId), DeviceResultDTO.class);
        if (!deviceId.equals(device.clientId()) || !stationId.equals(device.stationId()) || !Boolean.TRUE.equals(device.active())) {
            return denied("DEVICE_DISABLED_OR_MISMATCH");
        }
        var station = client.post(URI.create(stationUrl + "/internal/stations/" + stationId + "/checks"),
                new StationRequestDTO(deviceId), StationResultDTO.class);
        if (!stationId.equals(station.stationId()) || station.factoryId() == null || station.direction() == null
                || !java.util.Set.of("ENTRY", "EXIT").contains(station.direction()) || station.allowed() == null) {
            throw invalidResponse();
        }
        if (!station.allowed()) {
            return denied("STATION_ACCESS_DENIED");
        }
        var qr = client.post(URI.create(qrUrl + "/internal/qr/verifications"),
                new QrRequestDTO(qrToken, station.factoryId()), QrResultDTO.class);
        if (!Boolean.TRUE.equals(qr.valid())) {
            return denied("INVALID_OR_REVOKED_QR");
        }
        if (qr.employeeId() == null || qr.qrId() == null || qr.expiresAt() == null
                || !station.factoryId().equals(qr.factoryId())) {
            throw invalidResponse();
        }
        var permission = client.post(URI.create(identityUrl + "/internal/access-checks"),
                new PermissionRequestDTO(qr.employeeId(), station.factoryId()), PermissionResultDTO.class);
        if (!qr.employeeId().equals(permission.employeeId()) || !station.factoryId().equals(permission.factoryId())
                || permission.allowed() == null) {
            throw invalidResponse();
        }
        return new VerifiedAccess(qr.employeeId(), station.factoryId(), station.direction(), qr.qrId(),
                qr.expiresAt(), permission.allowed(), permission.allowed() ? "ALLOWED" : "SITE_ACCESS_DENIED");
    }

    private VerifiedAccess denied(String reason) {
        return new VerifiedAccess(null, null, null, null, null, false, reason);
    }

    private ResponseStatusException invalidResponse() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVALID_DEPENDENCY_RESPONSE");
    }
}
