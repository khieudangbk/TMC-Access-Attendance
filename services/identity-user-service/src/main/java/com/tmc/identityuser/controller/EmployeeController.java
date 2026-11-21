package com.tmc.identityuser.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.identityuser.dto.request.CreateEmployeeRequestDTO;
import com.tmc.identityuser.dto.request.PermissionsRequestDTO;
import com.tmc.identityuser.dto.request.ProfileRequestDTO;
import com.tmc.identityuser.dto.request.StatusRequestDTO;
import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.repository.IdentityStore;
import com.tmc.identityuser.service.IdentityService;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final IdentityService identityService;
    private final IdentityStore identityStore;

    public EmployeeController(IdentityService identityService, IdentityStore identityStore) {
        this.identityService = identityService;
        this.identityStore = identityStore;
    }

    @PostMapping
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    Employee create(@Valid @RequestBody CreateEmployeeRequestDTO request, Authentication authentication) {
        return identityService.createEmployee(request.employeeCode(), request.fullName(), request.username(),
                request.password(), request.roles(), request.factoryIds(), authentication.getName());
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("@identityAccess.canRead(authentication, #employeeId)")
    Employee employee(@PathVariable UUID employeeId) {
        return identityStore.employee(employeeId);
    }

    @GetMapping
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    com.tmc.api.PageData<Employee> employees(@RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 100 || offset < 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_PAGINATION");
        }
        return new com.tmc.api.PageData<>(identityStore.employees(limit, offset), limit, offset, identityStore.accountCount());
    }

    @PutMapping("/{employeeId}/profile")
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    Employee profile(@PathVariable UUID employeeId, @Valid @RequestBody ProfileRequestDTO request, Authentication authentication) {
        return identityService.updateProfile(employeeId, request.fullName(), authentication.getName());
    }

    @PutMapping("/{employeeId}/permissions")
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    Employee permissions(@PathVariable UUID employeeId, @Valid @RequestBody PermissionsRequestDTO request,
            Authentication authentication) {
        return identityService.updatePermissions(employeeId, request.roles(), request.factoryIds(), authentication.getName());
    }

    @PutMapping("/{employeeId}/status")
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    Employee status(@PathVariable UUID employeeId, @Valid @RequestBody StatusRequestDTO request, Authentication authentication) {
        return identityService.setActive(employeeId, request.active(), authentication.getName());
    }
}
