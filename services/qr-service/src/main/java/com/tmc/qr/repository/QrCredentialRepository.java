package com.tmc.qr.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.qr.entity.QrCredentialEntity;

public interface QrCredentialRepository extends JpaRepository<QrCredentialEntity, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update QrCredentialEntity credential set credential.revokedAt = :now
            where credential.employeeId = :employeeId and credential.factoryId = :factoryId and credential.revokedAt is null
            """)
    int revoke(UUID employeeId, UUID factoryId, Instant now);

    @Query("""
            select count(credential) > 0 from QrCredentialEntity credential
            where credential.qrId = :qrId and credential.employeeId = :employeeId and credential.factoryId = :factoryId
            and credential.issuedAt = :issuedAt and credential.expiresAt = :expiresAt and credential.keyId = :keyId
            and credential.revokedAt is null and credential.expiresAt > :now
            """)
    boolean matches(UUID qrId, UUID employeeId, UUID factoryId, Instant issuedAt, Instant expiresAt, String keyId, Instant now);
}
