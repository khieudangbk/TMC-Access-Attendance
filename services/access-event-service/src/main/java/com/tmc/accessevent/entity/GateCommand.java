package com.tmc.accessevent.entity;

import java.time.Instant;
import java.util.UUID;

public record GateCommand(UUID commandId, UUID scanId, UUID stationId, UUID employeeId,
        UUID factoryId, String direction, Instant expiresAt, Instant openedAt, Outcome outcome) {

    public enum Outcome {
        PENDING, OPENED, FAILED, UNKNOWN, PASSED
    }
}
