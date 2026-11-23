package com.tmc.qr.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "qr_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class QrCredentialEntity {
    @Id
    @Column(name = "qr_id")
    private UUID qrId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "factory_id")
    private UUID factoryId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "key_id", length = 100)
    private String keyId;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
