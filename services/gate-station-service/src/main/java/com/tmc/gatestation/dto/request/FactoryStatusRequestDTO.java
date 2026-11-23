package com.tmc.gatestation.dto.request;

import jakarta.validation.constraints.NotNull;

public record FactoryStatusRequestDTO(@NotNull Boolean active) {
}
