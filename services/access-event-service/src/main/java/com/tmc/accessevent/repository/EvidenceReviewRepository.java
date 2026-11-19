package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tmc.accessevent.dto.response.EvidenceDTO;
import com.tmc.accessevent.dto.response.ReviewDTO;
import com.tmc.accessevent.entity.EvidenceReviewEntity;
import com.tmc.accessevent.entity.GateCommand;

@Repository
@Transactional
public class EvidenceReviewRepository {
    private static final String JOINS = """
            from ConfirmationEntity confirmation join GateCommandEntity command on command.commandId = confirmation.commandId
            join ScanRequestEntity request on request.scanId = command.scanId
            """;
    private static final String EVIDENCE = """
            select new com.tmc.accessevent.dto.response.EvidenceDTO(confirmation.confirmationId, confirmation.commandId,
                command.scanId, command.stationId, command.employeeId, command.factoryId, request.deviceId,
                confirmation.reportedOutcome, confirmation.occurredAt, confirmation.receivedAt,
                confirmation.reviewRequired, confirmation.reason)
            """ + JOINS;
    private static final String PENDING = """
            where command.factoryId = :factoryId and (:stationId is null or command.stationId = :stationId)
            and confirmation.reviewRequired = true and not exists
                (select review.decisionId from EvidenceReviewEntity review where review.confirmationId = confirmation.confirmationId)
            """;
    private final EntityManager entityManager;
    private final EvidenceReviewJpaRepository reviews;
    private final GateCommandRepository commands;

    public EvidenceReviewRepository(EntityManager entityManager, EvidenceReviewJpaRepository reviews, GateCommandRepository commands) {
        this.entityManager = entityManager;
        this.reviews = reviews;
        this.commands = commands;
    }

    public List<EvidenceDTO> pending(UUID factoryId, UUID stationId, int limit, int offset) {
        return entityManager.createQuery(EVIDENCE + PENDING + " order by confirmation.receivedAt, confirmation.confirmationId", EvidenceDTO.class)
                .setParameter("factoryId", factoryId).setParameter("stationId", stationId).setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countPending(UUID factoryId, UUID stationId) {
        return entityManager.createQuery("select count(confirmation) " + JOINS + PENDING, Long.class)
                .setParameter("factoryId", factoryId).setParameter("stationId", stationId).getSingleResult();
    }

    public List<ReviewDTO> decision(UUID confirmationId) {
        return reviews.findByConfirmationId(confirmationId).stream().map(this::model).toList();
    }

    public List<EvidenceDTO> evidence(UUID confirmationId) {
        return entityManager.createQuery(EVIDENCE + " where confirmation.confirmationId = :confirmationId", EvidenceDTO.class)
                .setParameter("confirmationId", confirmationId).getResultList();
    }

    public List<ReviewDTO> priorDecisions(UUID confirmationId, UUID decisionId) {
        return reviews.findByConfirmationIdOrDecisionId(confirmationId, decisionId).stream().map(this::model).toList();
    }

    public int insertDecision(UUID decisionId, UUID confirmationId, UUID actor, boolean approved, String reason,
            Instant decidedAt, UUID eventId, String effect) {
        return reviews.insert(decisionId, confirmationId, actor, approved, reason, decidedAt, eventId, effect);
    }

    public int setOutcome(EvidenceDTO evidence, GateCommand command) {
        return commands.findById(command.commandId()).map(entity -> {
            entity.setOutcome(evidence.outcome());
            entityManager.flush();
            return 1;
        }).orElse(0);
    }

    public int audit(UUID decisionId, UUID actor, UUID confirmationId, boolean approved, UUID factoryId, Instant decidedAt) {
        return execute("""
                INSERT INTO integration_outbox(event_id, routing_key, payload) VALUES (?1, 'audit.access-event-service',
                    jsonb_build_object('eventId', CAST(?2 AS text), 'eventType', 'AUDIT_RECORDED', 'source', 'access-event-service',
                        'actor', CAST(?3 AS text), 'entityId', CAST(?4 AS text), 'action', CAST(?5 AS text), 'factoryId', CAST(?6 AS text),
                        'occurredAt', CAST(?7 AS timestamptz), 'version', 1))
                """, decisionId, decisionId.toString(), actor.toString(), confirmationId.toString(),
                approved ? "LATE_EVIDENCE_APPROVED" : "LATE_EVIDENCE_REJECTED", factoryId.toString(), decidedAt);
    }

    public int insertEvent(UUID eventId, GateCommand command, String type, EvidenceDTO evidence) {
        return execute("""
                INSERT INTO access_events (event_id, scan_id, station_id, employee_id, factory_id, direction,
                    event_type, occurred_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                """,
                eventId, command.scanId(), command.stationId(), command.employeeId(), command.factoryId(), command.direction(),
                type, evidence.occurredAt());
    }

    public int insertOutbox(UUID eventId, String type, GateCommand command, EvidenceDTO evidence, UUID actor, UUID decisionId) {
        return execute("""
                INSERT INTO outbox_events(event_id, event_type, payload) VALUES (?1, ?2, jsonb_build_object(
                    'eventId', CAST(?3 AS text), 'scanId', CAST(?4 AS text), 'stationId', CAST(?5 AS text), 'employeeId', CAST(?6 AS text),
                    'factoryId', CAST(?7 AS text), 'direction', CAST(?8 AS text), 'eventType', CAST(?9 AS text), 'occurredAt', CAST(?10 AS timestamptz),
                    'version', 1, 'actor', CAST(?11 AS text), 'reviewDecisionId', CAST(?12 AS text), 'sourceConfirmationId', CAST(?13 AS text)))
                """, eventId, type, eventId.toString(), command.scanId().toString(), command.stationId().toString(),
                command.employeeId().toString(), command.factoryId().toString(), command.direction(), type,
                evidence.occurredAt(), actor.toString(), decisionId.toString(), evidence.confirmationId().toString());
    }

    private int execute(String sql, Object... parameters) {
        entityManager.flush();
        Query query = entityManager.createNativeQuery(sql);
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
        return query.executeUpdate();
    }

    private ReviewDTO model(EvidenceReviewEntity review) {
        return new ReviewDTO(review.getDecisionId(), review.getConfirmationId(), review.getActorId(), review.isApproved(),
                review.getReason(), review.getDecidedAt(), review.getEventId(), review.getEffect());
    }
}
