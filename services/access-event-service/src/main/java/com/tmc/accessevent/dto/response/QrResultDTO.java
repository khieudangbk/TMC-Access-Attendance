package com.tmc.accessevent.dto.response;

import java.time.Instant;
import java.util.UUID;

public record QrResultDTO(Boolean valid, String reason, UUID qrId, UUID employeeId, UUID factoryId, Instant expiresAt) {
}
