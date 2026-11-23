package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.GateCommand;

public interface AccessStore {

    boolean claimScan(UUID scanId, UUID stationId, String deviceId, String fingerprint);

    ScanDecisionDTO existingDecision(UUID scanId, String fingerprint);

    boolean redeemQr(UUID qrId, UUID scanId, Instant expiresAt);

    void saveDecision(ScanDecisionDTO decision, Instant occurredAt);

    GateCommand lockCommand(UUID commandId);

    Optional<ConfirmationResultDTO> existingConfirmation(UUID confirmationId, String fingerprint);

    void saveConfirmation(UUID commandId, String fingerprint, Instant occurredAt, ConfirmationResultDTO result);

    void saveOutcome(GateCommand command, GateCommand.Outcome outcome, Instant occurredAt);
}
