package com.tmc.qr.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IssuedQrDTO(UUID qrId, String token, Instant expiresAt) {
    @Override
    public String toString() {
        return "IssuedQrDTO[qrId=" + qrId + ", token=REDACTED, expiresAt=" + expiresAt + "]";
    }
}
