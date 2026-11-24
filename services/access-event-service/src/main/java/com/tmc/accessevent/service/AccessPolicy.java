package com.tmc.accessevent.service;

import java.util.UUID;

import com.tmc.accessevent.entity.VerifiedAccess;

public interface AccessPolicy {

    // Resolve trusted station configuration and verify QR, employee and site permissions.
    VerifiedAccess verify(UUID stationId, String deviceId, String qrToken);
}
