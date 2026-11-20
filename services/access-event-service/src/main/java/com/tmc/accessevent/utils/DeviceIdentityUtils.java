package com.tmc.accessevent.utils;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class DeviceIdentityUtils {

    private DeviceIdentityUtils() {
    }

    public static UUID stationId(Jwt jwt) {
        try {
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(jwt.getClaimAsString("station_id"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_STATION_REQUIRED");
        }
    }
}
