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
@Table(name = "access_evidence_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class EvidenceReviewEntity {
    @Id
    @Column(name = "decision_id")
    private UUID decisionId;

    @Column(name = "confirmation_id")
    private UUID confirmationId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "approved")
    private boolean approved;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "effect", length = 40)
    private String effect;
}
