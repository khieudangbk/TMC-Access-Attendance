package com.tmc.accessevent.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequestDTO(@NotNull UUID decisionId, @NotNull Boolean approved, @NotBlank @Size(max = 500) String reason) {
}
