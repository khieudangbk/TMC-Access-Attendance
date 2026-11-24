package com.tmc.accessevent.entity;

import java.time.Instant;
import java.util.UUID;

public record VerifiedAccess(UUID employeeId, UUID factoryId, String direction, UUID qrId,
        Instant qrExpiresAt, boolean allowed, String reason) {
}
