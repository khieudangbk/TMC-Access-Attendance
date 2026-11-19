package com.tmc.gatestation.dto.response;

import java.util.UUID;

import com.tmc.gatestation.entity.Direction;

public record StationCheckDTO(UUID stationId, UUID factoryId, Direction direction, boolean allowed, String reason, long revision) {
}
