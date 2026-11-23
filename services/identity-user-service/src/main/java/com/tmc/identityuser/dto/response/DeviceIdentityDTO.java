package com.tmc.identityuser.dto.response;

import java.util.UUID;

public record DeviceIdentityDTO(String clientId, UUID stationId, boolean active) {
}
