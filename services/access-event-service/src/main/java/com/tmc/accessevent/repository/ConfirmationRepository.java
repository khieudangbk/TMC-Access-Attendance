package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.accessevent.entity.ConfirmationEntity;

public interface ConfirmationRepository extends JpaRepository<ConfirmationEntity, UUID> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO command_confirmations (confirmation_id, command_id, fingerprint,
                reported_outcome, occurred_at, review_required, reason)
            VALUES (:confirmationId, :commandId, :fingerprint, :outcome, :occurredAt, :reviewRequired, :reason)
            ON CONFLICT (confirmation_id) DO NOTHING
            """, nativeQuery = true)
    int insert(UUID confirmationId, UUID commandId, String fingerprint, String outcome, Instant occurredAt,
            boolean reviewRequired, String reason);
}
