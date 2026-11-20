package com.tmc.accessevent.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.GateCommand;
import com.tmc.accessevent.entity.GateCommand.Outcome;
import com.tmc.accessevent.repository.AccessStore;
import com.tmc.accessevent.service.AccessPolicy;
import com.tmc.accessevent.service.AccessService;
import com.tmc.accessevent.utils.HashUtils;

@Service("accessService")
public class AccessServiceImpl implements AccessService {

    private static final Duration COMMAND_TTL = Duration.ofSeconds(10);
    private static final Duration MAX_CONFIRMATION_DELAY = Duration.ofSeconds(30);
    private final AccessStore accessStore;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public AccessServiceImpl(AccessStore accessStore, AccessPolicy accessPolicy, Clock clock) {
        this.accessStore = accessStore;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Transactional(timeout = 2)
    @Override
    public ScanDecisionDTO scan(UUID scanId, UUID stationId, String deviceId, String qrToken) {
        String fingerprint = HashUtils.sha256(stationId + "\n" + deviceId + "\n" + qrToken);
        if (!accessStore.claimScan(scanId, stationId, deviceId, fingerprint)) {
            return accessStore.existingDecision(scanId, fingerprint);
        }
        var verifiedAccess = accessPolicy.verify(stationId, deviceId, qrToken);
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        boolean allowed = verifiedAccess.allowed();
        String reason = verifiedAccess.reason();
        if (allowed && (verifiedAccess.qrExpiresAt() == null
                || !verifiedAccess.qrExpiresAt().isAfter(now))) {
            allowed = false;
            reason = "QR_EXPIRED";
        }
        if (allowed && !accessStore.redeemQr(verifiedAccess.qrId(), scanId,
                verifiedAccess.qrExpiresAt())) {
            allowed = false;
            reason = "QR_REPLAYED";
        }
        var decision = new ScanDecisionDTO(scanId, stationId, verifiedAccess.employeeId(),
                verifiedAccess.factoryId(), verifiedAccess.direction(), allowed, reason,
                allowed ? UUID.randomUUID() : null, allowed ? now.plus(COMMAND_TTL) : null);
        accessStore.saveDecision(decision, now);
        return decision;
    }

    @Transactional
    @Override
    public ConfirmationResultDTO confirm(UUID commandId, UUID stationId, UUID confirmationId,
            Outcome outcome, Instant occurredAt) {
        GateCommand command = accessStore.lockCommand(commandId);
        if (!command.stationId().equals(stationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "STATION_MISMATCH");
        }
        String fingerprint = HashUtils.sha256(commandId + "\n" + outcome + "\n" + occurredAt);
        var previousConfirmation = accessStore.existingConfirmation(confirmationId, fingerprint);
        if (previousConfirmation.isPresent()) {
            return previousConfirmation.get();
        }
        Instant receivedAt = clock.instant();
        if (outcome == Outcome.PENDING || occurredAt.isAfter(receivedAt.plusSeconds(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CONFIRMATION");
        }
        if (occurredAt.isBefore(command.expiresAt().minus(COMMAND_TTL).minusSeconds(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CONFIRMATION_TIME");
        }
        if (Duration.between(occurredAt, receivedAt).compareTo(MAX_CONFIRMATION_DELAY) > 0
                || (outcome == Outcome.OPENED && !command.expiresAt().isAfter(receivedAt))) {
            var result = new ConfirmationResultDTO(confirmationId, outcome, true, "LATE_EVIDENCE_REQUIRES_REVIEW");
            accessStore.saveConfirmation(commandId, fingerprint, occurredAt, result);
            return result;
        }
        if (command.outcome() == outcome) {
            var result = new ConfirmationResultDTO(confirmationId, outcome, false, "ALREADY_RECORDED");
            accessStore.saveConfirmation(commandId, fingerprint, occurredAt, result);
            return result;
        }
        if (command.outcome() == Outcome.PASSED || command.outcome() == Outcome.FAILED
                || (outcome == Outcome.PASSED && command.outcome() != Outcome.OPENED)
                || (command.outcome() == Outcome.OPENED && outcome != Outcome.PASSED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INVALID_COMMAND_TRANSITION");
        }
        if (outcome == Outcome.OPENED && !command.expiresAt().isAfter(occurredAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "COMMAND_EXPIRED");
        }
        if (outcome == Outcome.PASSED && occurredAt.isBefore(command.openedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PASSAGE_BEFORE_GATE_OPENED");
        }
        accessStore.saveOutcome(command, outcome, occurredAt);
        var result = new ConfirmationResultDTO(confirmationId, outcome, false, "RECORDED");
        accessStore.saveConfirmation(commandId, fingerprint, occurredAt, result);
        return result;
    }
}
