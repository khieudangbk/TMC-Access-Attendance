package com.tmc.accessevent.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ScanDecisionDTO(UUID scanId, UUID stationId, UUID employeeId, UUID factoryId,
        String direction, boolean allowed, String reason, UUID commandId, Instant commandExpiresAt) {
}
