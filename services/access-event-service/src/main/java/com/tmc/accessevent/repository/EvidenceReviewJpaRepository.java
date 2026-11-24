package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.accessevent.entity.EvidenceReviewEntity;

public interface EvidenceReviewJpaRepository extends JpaRepository<EvidenceReviewEntity, UUID> {
    List<EvidenceReviewEntity> findByConfirmationId(UUID confirmationId);

    List<EvidenceReviewEntity> findByConfirmationIdOrDecisionId(UUID confirmationId, UUID decisionId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO access_evidence_reviews VALUES (:decisionId, :confirmationId, :actor, :approved,
                :reason, :decidedAt, :eventId, :effect) ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insert(UUID decisionId, UUID confirmationId, UUID actor, boolean approved, String reason,
            Instant decidedAt, UUID eventId, String effect);
}
