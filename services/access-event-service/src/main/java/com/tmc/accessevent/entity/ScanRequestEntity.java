package com.tmc.accessevent.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scan_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class ScanRequestEntity {
    @Id
    @Column(name = "scan_id")
    private UUID scanId;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "factory_id")
    private UUID factoryId;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "allowed")
    private Boolean allowed;

    @Column(name = "reason", length = 80)
    private String reason;

    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "command_expires_at")
    private Instant commandExpiresAt;
}
