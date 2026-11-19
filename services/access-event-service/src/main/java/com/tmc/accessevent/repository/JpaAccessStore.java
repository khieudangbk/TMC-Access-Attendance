package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.GateCommand;
import com.tmc.accessevent.entity.GateCommand.Outcome;
import com.tmc.accessevent.entity.GateCommandEntity;
import com.tmc.accessevent.entity.ScanRequestEntity;

@Repository
@Transactional
public class JpaAccessStore implements AccessStore {
    private final EntityManager entityManager;
    private final ScanRequestRepository scans;
    private final GateCommandRepository commands;
    private final ConfirmationRepository confirmations;
    private final AccessEventRepository events;

    public JpaAccessStore(EntityManager entityManager, ScanRequestRepository scans, GateCommandRepository commands,
            ConfirmationRepository confirmations, AccessEventRepository events) {
        this.entityManager = entityManager;
        this.scans = scans;
        this.commands = commands;
        this.confirmations = confirmations;
        this.events = events;
    }

    @Override
    public boolean claimScan(UUID scanId, UUID stationId, String deviceId, String fingerprint) {
        return scans.claim(scanId, stationId, deviceId, fingerprint) == 1;
    }

    @Override
    public ScanDecisionDTO existingDecision(UUID scanId, String fingerprint) {
        ScanRequestEntity scan = scans.findById(scanId).orElseThrow();
        if (!fingerprint.equals(scan.getFingerprint())) {
            throw conflict();
        }
        return new ScanDecisionDTO(scan.getScanId(), scan.getStationId(), scan.getEmployeeId(), scan.getFactoryId(),
                scan.getDirection(), Boolean.TRUE.equals(scan.getAllowed()), scan.getReason(), scan.getCommandId(), scan.getCommandExpiresAt());
    }

    @Override
    public boolean redeemQr(UUID qrId, UUID scanId, Instant expiresAt) {
        return scans.redeemQr(qrId, scanId, expiresAt) == 1;
    }

    @Override
    public void saveDecision(ScanDecisionDTO decision, Instant occurredAt) {
        ScanRequestEntity scan = scans.findById(decision.scanId()).orElseThrow();
        scan.setEmployeeId(decision.employeeId());
        scan.setFactoryId(decision.factoryId());
        scan.setDirection(decision.direction());
        scan.setAllowed(decision.allowed());
        scan.setReason(decision.reason());
        scan.setCommandId(decision.commandId());
        scan.setCommandExpiresAt(decision.commandExpiresAt());
        if (decision.allowed()) {
            entityManager.persist(new GateCommandEntity(decision.commandId(), decision.scanId(), decision.stationId(),
                    decision.employeeId(), decision.factoryId(), decision.direction(), decision.commandExpiresAt(), Outcome.PENDING));
        }
        appendEvent(decision.scanId(), decision.stationId(), decision.employeeId(), decision.factoryId(),
                decision.direction(), decision.allowed() ? "ACCESS_ALLOWED" : "ACCESS_DENIED", occurredAt);
    }

    @Override
    public GateCommand lockCommand(UUID commandId) {
        GateCommandEntity command = commands.findLocked(commandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "COMMAND_NOT_FOUND"));
        return new GateCommand(command.getCommandId(), command.getScanId(), command.getStationId(), command.getEmployeeId(),
                command.getFactoryId(), command.getDirection(), command.getExpiresAt(), events.openedAt(command.getScanId()).orElse(null),
                command.getOutcome());
    }

    @Override
    public Optional<ConfirmationResultDTO> existingConfirmation(UUID confirmationId, String fingerprint) {
        return confirmations.findById(confirmationId).map(confirmation -> {
            if (!fingerprint.equals(confirmation.getFingerprint())) {
                throw conflict();
            }
            return new ConfirmationResultDTO(confirmationId, confirmation.getReportedOutcome() == null ? Outcome.UNKNOWN : confirmation.getReportedOutcome(),
                    confirmation.isReviewRequired(), confirmation.getReason());
        });
    }

    @Override
    public void saveConfirmation(UUID commandId, String fingerprint, Instant occurredAt, ConfirmationResultDTO result) {
        if (confirmations.insert(result.confirmationId(), commandId, fingerprint, result.outcome().name(), occurredAt,
                result.reviewRequired(), result.reason()) == 0) {
            throw conflict();
        }
    }

    @Override
    public void saveOutcome(GateCommand command, Outcome outcome, Instant occurredAt) {
        commands.findById(command.commandId()).orElseThrow().setOutcome(outcome);
        String eventType = outcome == Outcome.PASSED ? "PASSAGE_CONFIRMED" : "GATE_" + outcome.name();
        appendEvent(command.scanId(), command.stationId(), command.employeeId(), command.factoryId(), command.direction(), eventType, occurredAt);
    }

    private void appendEvent(UUID scanId, UUID stationId, UUID employeeId, UUID factoryId,
            String direction, String eventType, Instant occurredAt) {
        UUID eventId = UUID.randomUUID();
        // Flush the decision/command before inserting immutable evidence and its outbox entry.
        entityManager.flush();
        entityManager.createNativeQuery("""
                INSERT INTO access_events (event_id, scan_id, station_id, employee_id,
                    factory_id, direction, event_type, occurred_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                """).setParameter(1, eventId).setParameter(2, scanId).setParameter(3, stationId).setParameter(4, employeeId)
                .setParameter(5, factoryId).setParameter(6, direction).setParameter(7, eventType).setParameter(8, occurredAt).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO outbox_events (event_id, event_type, payload)
                SELECT event_id, event_type, jsonb_build_object('eventId', event_id, 'scanId', scan_id,
                    'stationId', station_id, 'employeeId', employee_id, 'factoryId', factory_id,
                    'direction', direction, 'eventType', event_type, 'occurredAt', occurred_at, 'version', 1,
                    'actor', (SELECT device_id FROM scan_requests WHERE scan_id = access_events.scan_id))
                FROM access_events WHERE event_id = ?1
                """).setParameter(1, eventId).executeUpdate();
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT");
    }
}
