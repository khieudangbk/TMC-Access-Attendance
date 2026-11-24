package com.tmc.gatestation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StatusRequestDTO(@NotNull Boolean active, @Min(1) long expectedRevision) {
}
