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
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "access_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class AccessEventEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "scan_id")
    private UUID scanId;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "factory_id")
    private UUID factoryId;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "event_type", length = 30)
    private String eventType;

    @Column(name = "occurred_at")
    private Instant occurredAt;
}
