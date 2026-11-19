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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tmc.accessevent.entity.GateCommand.Outcome;

@Entity
@Immutable
@Table(name = "command_confirmations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class ConfirmationEntity {
    @Id
    @Column(name = "confirmation_id")
    private UUID confirmationId;

    @Column(name = "command_id")
    private UUID commandId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_outcome", length = 12)
    private Outcome reportedOutcome;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "review_required")
    private boolean reviewRequired;

    @Column(name = "reason", length = 80)
    private String reason;
}
