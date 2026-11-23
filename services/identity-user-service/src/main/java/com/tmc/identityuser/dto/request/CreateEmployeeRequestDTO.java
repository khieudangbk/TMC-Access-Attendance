package com.tmc.identityuser.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.tmc.identityuser.entity.Employee.Role;

public record CreateEmployeeRequestDTO(
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{1,50}") String employeeCode,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._@-]{3,100}") String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotEmpty Set<@NotNull Role> roles, @NotNull @Size(max = 100) Set<@NotNull UUID> factoryIds) {
    @Override
    public String toString() {
        return "CreateEmployeeRequestDTO[REDACTED]";
    }
}
