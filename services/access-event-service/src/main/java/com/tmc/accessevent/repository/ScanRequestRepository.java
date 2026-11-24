package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.accessevent.entity.ScanRequestEntity;

public interface ScanRequestRepository extends JpaRepository<ScanRequestEntity, UUID> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO scan_requests (scan_id, station_id, device_id, fingerprint)
            VALUES (:scanId, :stationId, :deviceId, :fingerprint) ON CONFLICT (scan_id) DO NOTHING
            """, nativeQuery = true)
    int claim(UUID scanId, UUID stationId, String deviceId, String fingerprint);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO qr_redemptions (qr_id, scan_id, expires_at) VALUES (:qrId, :scanId, :expiresAt)
            ON CONFLICT (qr_id) DO NOTHING
            """, nativeQuery = true)
    int redeemQr(UUID qrId, UUID scanId, Instant expiresAt);
}
