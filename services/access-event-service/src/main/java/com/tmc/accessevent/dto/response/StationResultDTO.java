package com.tmc.accessevent.dto.response;

import java.util.UUID;

public record StationResultDTO(UUID stationId, UUID factoryId, String direction, Boolean allowed, String reason, Long revision) {
}
