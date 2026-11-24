package com.tmc.gatestation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class StationEntity {
    @Id
    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "factory_id")
    private UUID factoryId;

    @Column(name = "station_code", length = 50)
    private String stationCode;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 5)
    private Direction direction;

    @Column(name = "controller_reference", length = 150)
    private String controllerReference;

    @Column(name = "active")
    private boolean active;

    @Version
    @Column(name = "revision")
    private long revision;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
