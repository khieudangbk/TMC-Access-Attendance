package com.tmc.accessevent.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.dto.response.EvidenceDTO;
import com.tmc.accessevent.dto.response.ReviewDTO;
import com.tmc.accessevent.dto.response.ReviewDetailDTO;
import com.tmc.accessevent.entity.GateCommand;
import com.tmc.accessevent.entity.GateCommand.Outcome;
import com.tmc.accessevent.repository.AccessStore;
import com.tmc.accessevent.repository.EvidenceReviewRepository;
import com.tmc.accessevent.service.EvidenceReviewService;
import com.tmc.api.PageData;

@Service("evidenceReviewService")
public class EvidenceReviewServiceImpl implements EvidenceReviewService {

    private final EvidenceReviewRepository repository;
    private final AccessStore store;
    private final Clock clock;

    public EvidenceReviewServiceImpl(EvidenceReviewRepository repository, AccessStore store, Clock clock) {
        this.repository = repository;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public PageData<EvidenceDTO> pending(UUID factoryId, UUID stationId, int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION");
        }

        List<EvidenceDTO> items = repository.pending(factoryId, stationId, limit, offset);
        long total = repository.countPending(factoryId, stationId);
        return new PageData<>(items, limit, offset, total);
    }

    @Override
    public ReviewDetailDTO detail(UUID factoryId, UUID confirmationId) {
        EvidenceDTO evidence = repository.evidence(confirmationId).stream().findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND"));
        if (!factoryId.equals(evidence.factoryId())) {
            throw error(HttpStatus.FORBIDDEN, "EVIDENCE_FACTORY_MISMATCH");
        }
        ReviewDTO decision = repository.decision(confirmationId).stream().findFirst().orElse(null);
        return new ReviewDetailDTO(evidence, decision);
    }

    @Transactional
    @Override
    public ReviewDTO decide(UUID factoryId, UUID confirmationId, UUID decisionId, boolean approved, String reason, UUID actor) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw error(HttpStatus.BAD_REQUEST, "REVIEW_REASON_REQUIRED");
        }
        EvidenceDTO evidence = repository.evidence(confirmationId).stream().findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND"));
        if (!factoryId.equals(evidence.factoryId())) {
            throw error(HttpStatus.FORBIDDEN, "EVIDENCE_FACTORY_MISMATCH");
        }
        GateCommand command = store.lockCommand(evidence.commandId());
        var prior = repository.priorDecisions(confirmationId, decisionId);
        if (!prior.isEmpty()) {
            ReviewDTO previous = prior.getFirst();
            if (previous.decisionId().equals(decisionId) && previous.confirmationId().equals(confirmationId)
                    && previous.actorId().equals(actor) && previous.approved() == approved && previous.reason().equals(reason)) {
                return previous;
            }
            throw error(HttpStatus.CONFLICT, "REVIEW_ALREADY_DECIDED_OR_ID_REUSED");
        }
        if (!evidence.reviewRequired()) {
            throw error(HttpStatus.CONFLICT, "EVIDENCE_NOT_PENDING_REVIEW");
        }
        if (actor.equals(evidence.employeeId()) || actor.toString().equals(evidence.deviceId())) {
            throw error(HttpStatus.FORBIDDEN, "INDEPENDENT_REVIEWER_REQUIRED");
        }
        UUID eventId = null;
        String effect = "REJECTED";
        if (approved) {
            validateApproval(command, evidence);
            if (command.outcome() == evidence.outcome()) {
                effect = "ALREADY_RECORDED";
            } else {
                eventId = apply(command, evidence, decisionId, actor);
                effect = "APPLIED";
            }
        }
        Instant decidedAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        int inserted = repository.insertDecision(decisionId, confirmationId, actor, approved, reason, decidedAt, eventId, effect);
        if (inserted == 0) {
            throw error(HttpStatus.CONFLICT, "REVIEW_ALREADY_DECIDED_OR_ID_REUSED");
        }
        repository.audit(decisionId, actor, confirmationId, approved, factoryId, decidedAt);
        return new ReviewDTO(decisionId, confirmationId, actor, approved, reason, decidedAt, eventId, effect);
    }

    private void validateApproval(GateCommand command, EvidenceDTO evidence) {
        Outcome outcome = evidence.outcome();
        if (outcome == null || evidence.occurredAt() == null) {
            throw error(HttpStatus.CONFLICT, "LEGACY_EVIDENCE_INCOMPLETE");
        }
        if (outcome == Outcome.OPENED && !evidence.occurredAt().isBefore(command.expiresAt())) {
            throw error(HttpStatus.CONFLICT, "OPENED_AFTER_COMMAND_EXPIRED");
        }
        if (outcome == Outcome.PASSED) {
            if (command.openedAt() == null || evidence.occurredAt().isBefore(command.openedAt())) {
                throw error(HttpStatus.CONFLICT, "VALID_OPEN_EVIDENCE_REQUIRED");
            }
        }
        if (command.outcome() == outcome) {
            return;
        }
        if (command.outcome() == Outcome.FAILED || command.outcome() == Outcome.PASSED
                || command.outcome() == Outcome.OPENED && outcome != Outcome.PASSED
                || outcome == Outcome.PASSED && command.outcome() != Outcome.OPENED || outcome == Outcome.PENDING) {
            throw error(HttpStatus.CONFLICT, "INVALID_COMMAND_TRANSITION");
        }
    }

    private UUID apply(GateCommand command, EvidenceDTO evidence, UUID decisionId, UUID actor) {
        UUID eventId = UUID.randomUUID();
        String type = evidence.outcome() == Outcome.PASSED ? "PASSAGE_CONFIRMED" : "GATE_" + evidence.outcome().name();
        repository.setOutcome(evidence, command);
        repository.insertEvent(eventId, command, type, evidence);
        repository.insertOutbox(eventId, type, command, evidence, actor, decisionId);
        return eventId;
    }

    private static ResponseStatusException error(HttpStatus status, String reason) {
        return new ResponseStatusException(status, reason);
    }
}
