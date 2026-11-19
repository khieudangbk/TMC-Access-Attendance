package com.tmc.accessevent.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewDTO(UUID decisionId, UUID confirmationId, UUID actorId, boolean approved, String reason,
        Instant decidedAt, UUID eventId, String effect) {
}
