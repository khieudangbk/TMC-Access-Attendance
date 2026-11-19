package com.tmc.identityuser.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AccessCheckRequestDTO(@NotNull UUID employeeId, @NotNull UUID factoryId) {
}
