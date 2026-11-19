package com.tmc.identityuser.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tmc.identityuser.entity.Employee.Role;

public record PermissionsRequestDTO(@NotEmpty Set<@NotNull Role> roles,
        @NotNull @Size(max = 100) Set<@NotNull UUID> factoryIds) {
}
