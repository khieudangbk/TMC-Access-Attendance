package com.tmc.accessevent.dto.response;

import java.util.UUID;

public record DeviceResultDTO(String clientId, UUID stationId, Boolean active) {
}
