package com.tmc.accessevent.service;

import java.time.Instant;
import java.util.UUID;

import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.GateCommand.Outcome;

public interface AccessService {

    ScanDecisionDTO scan(UUID scanId, UUID stationId, String deviceId, String qrToken);

    ConfirmationResultDTO confirm(UUID commandId, UUID stationId, UUID confirmationId,
            Outcome outcome, Instant occurredAt);
}
