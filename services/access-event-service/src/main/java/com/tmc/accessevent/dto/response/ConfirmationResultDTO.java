package com.tmc.accessevent.dto.response;

import java.util.UUID;

import com.tmc.accessevent.entity.GateCommand;

public record ConfirmationResultDTO(UUID confirmationId, GateCommand.Outcome outcome,
        boolean reviewRequired, String reason) {
}
