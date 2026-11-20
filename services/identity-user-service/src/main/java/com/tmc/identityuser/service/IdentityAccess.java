package com.tmc.identityuser.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.repository.IdentityStore;

@Component("identityAccess")
public class IdentityAccess {

    private final IdentityStore identityStore;

    public IdentityAccess(IdentityStore identityStore) {
        this.identityStore = identityStore;
    }

    public boolean isAdmin(Authentication authentication) {
        try {
            var employee = identityStore.employee(UUID.fromString(authentication.getName()));
            return employee.active() && employee.roles().contains(Role.ADMIN);
        } catch (IllegalArgumentException | ResponseStatusException exception) {
            return false;
        }
    }

    public boolean canRead(Authentication authentication, UUID employeeId) {
        try {
            UUID actorId = UUID.fromString(authentication.getName());
            var actor = identityStore.employee(actorId);
            return actor.active() && (actorId.equals(employeeId) || actor.roles().contains(Role.ADMIN));
        } catch (IllegalArgumentException | ResponseStatusException exception) {
            return false;
        }
    }
}
