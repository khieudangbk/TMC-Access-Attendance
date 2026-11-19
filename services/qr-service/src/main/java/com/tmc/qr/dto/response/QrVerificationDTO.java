package com.tmc.qr.dto.response;

import java.time.Instant;
import java.util.UUID;

public record QrVerificationDTO(boolean valid, String reason, UUID qrId, UUID employeeId,
        UUID factoryId, Instant expiresAt) {

    public static QrVerificationDTO denied(String reason) {
        return new QrVerificationDTO(false, reason, null, null, null, null);
    }
}
