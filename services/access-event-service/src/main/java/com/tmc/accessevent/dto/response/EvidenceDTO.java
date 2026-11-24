package com.tmc.accessevent.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tmc.accessevent.entity.GateCommand.Outcome;

public record EvidenceDTO(UUID confirmationId, UUID commandId, UUID scanId, UUID stationId, UUID employeeId,
        UUID factoryId, String deviceId, Outcome outcome, Instant occurredAt, Instant receivedAt,
        boolean reviewRequired, String reason) {
}
