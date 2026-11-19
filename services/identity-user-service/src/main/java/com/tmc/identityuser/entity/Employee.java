package com.tmc.identityuser.entity;

import java.util.Set;
import java.util.UUID;

public record Employee(UUID employeeId, String employeeCode, String fullName, boolean active,
        Set<Role> roles, Set<UUID> factoryIds) {

    public enum Role {
        EMPLOYEE, SUPERVISOR, HR, SECURITY, ADMIN, EXECUTIVE
    }
}
