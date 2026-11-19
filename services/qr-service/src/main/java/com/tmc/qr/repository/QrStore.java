package com.tmc.qr.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.tmc.qr.entity.QrCredentialEntity;

@Repository
public class QrStore {
    private final EntityManager entityManager;
    private final QrCredentialRepository credentials;

    public QrStore(EntityManager entityManager, QrCredentialRepository credentials) {
        this.entityManager = entityManager;
        this.credentials = credentials;
    }

    public void lockEmployee(UUID employeeId) {
        entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(?1)")
                .setParameter(1, employeeId.getMostSignificantBits() ^ employeeId.getLeastSignificantBits()).getSingleResult();
    }

    public void revoke(UUID employeeId, UUID factoryId, Instant now) {
        credentials.revoke(employeeId, factoryId, now);
    }

    public void save(UUID qrId, UUID employeeId, UUID factoryId, Instant issuedAt, Instant expiresAt, String keyId) {
        entityManager.persist(new QrCredentialEntity(qrId, employeeId, factoryId, issuedAt, expiresAt, keyId, null));
        entityManager.flush();
        audit(employeeId, qrId, factoryId, "QR_ISSUED");
    }

    public boolean matches(UUID qrId, UUID employeeId, UUID factoryId, Instant issuedAt,
            Instant expiresAt, String keyId, Instant now) {
        return credentials.matches(qrId, employeeId, factoryId, issuedAt, expiresAt, keyId, now);
    }

    public void audit(UUID employeeId, UUID qrId, UUID factoryId, String eventType) {
        entityManager.createNativeQuery("""
                INSERT INTO qr_audit_events (event_id, employee_id, qr_id, factory_id, event_type) VALUES (?1, ?2, ?3, ?4, ?5)
                """).setParameter(1, UUID.randomUUID()).setParameter(2, employeeId).setParameter(3, qrId)
                .setParameter(4, factoryId).setParameter(5, eventType).executeUpdate();
    }
}
