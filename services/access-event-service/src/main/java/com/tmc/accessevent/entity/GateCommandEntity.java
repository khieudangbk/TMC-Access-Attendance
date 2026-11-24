package com.tmc.accessevent.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import com.tmc.accessevent.entity.GateCommand.Outcome;

@Entity
@Table(name = "gate_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class GateCommandEntity {
    @Id
    @Column(name = "command_id")
    private UUID commandId;

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

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 12)
    private Outcome outcome;
}
