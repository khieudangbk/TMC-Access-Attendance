package com.tmc.identityuser.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.tmc.identityuser.entity.Employee.Role;

public record AuthorizationRequestDTO(@NotNull UUID employeeId, @NotNull UUID factoryId,
        @NotEmpty Set<@NotNull Role> roles) {
}
