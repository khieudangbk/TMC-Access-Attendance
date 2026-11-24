package com.tmc.identityuser.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.identityuser.dto.response.AccessCheckDTO;
import com.tmc.identityuser.dto.request.AccessCheckRequestDTO;
import com.tmc.identityuser.dto.response.AuthorizationCheckDTO;
import com.tmc.identityuser.dto.request.AuthorizationRequestDTO;
import com.tmc.identityuser.dto.response.DeviceIdentityDTO;
import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.repository.IdentityStore;

@RestController
@RequestMapping("/internal")
public class InternalIdentityController {

    private final IdentityStore identityStore;

    public InternalIdentityController(IdentityStore identityStore) {
        this.identityStore = identityStore;
    }

    @PostMapping("/access-checks")
    @PreAuthorize("hasAuthority('SCOPE_identity.verify')")
    AccessCheckDTO check(@Valid @RequestBody AccessCheckRequestDTO request) {
        try {
            Employee employee = identityStore.employee(request.employeeId());
            boolean allowed = employee.active() && employee.factoryIds().contains(request.factoryId());
            return new AccessCheckDTO(employee.employeeId(), request.factoryId(), allowed,
                    !employee.active() ? "EMPLOYEE_DISABLED" : allowed ? "ALLOWED" : "SITE_ACCESS_DENIED");
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
            return new AccessCheckDTO(request.employeeId(), request.factoryId(), false, "EMPLOYEE_NOT_FOUND");
        }
    }

    @GetMapping("/devices/{clientId}")
    @PreAuthorize("hasAuthority('SCOPE_identity.verify')")
    DeviceIdentityDTO device(@PathVariable String clientId) {
        return identityStore.device(clientId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND"));
    }

    @PostMapping("/authorization-checks")
    @PreAuthorize("hasAuthority('SCOPE_identity.verify')")
    AuthorizationCheckDTO authorize(@Valid @RequestBody AuthorizationRequestDTO request) {
        try {
            Employee employee = identityStore.employee(request.employeeId());
            boolean allowed = employee.active() && (employee.roles().contains(Role.ADMIN)
                    || employee.factoryIds().contains(request.factoryId())
                    && request.roles().stream().anyMatch(employee.roles()::contains));
            return new AuthorizationCheckDTO(request.employeeId(), request.factoryId(), allowed);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
            return new AuthorizationCheckDTO(request.employeeId(), request.factoryId(), false);
        }
    }

    @GetMapping("/factories/{factoryId}/employees")
    @PreAuthorize("hasAuthority('SCOPE_identity.verify')")
    java.util.List<UUID> roster(@PathVariable UUID factoryId) {
        return identityStore.factoryEmployees(factoryId);
    }
}
