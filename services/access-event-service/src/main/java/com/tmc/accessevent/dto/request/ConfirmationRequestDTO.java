package com.tmc.accessevent.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.tmc.accessevent.entity.GateCommand.Outcome;

public record ConfirmationRequestDTO(@NotNull UUID confirmationId, @NotNull Outcome outcome,
        @NotNull Instant occurredAt) {
}
