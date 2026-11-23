package com.tmc.identityuser.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.identityuser.dto.response.DeviceIdentityDTO;
import com.tmc.identityuser.dto.request.DeviceRequestDTO;
import com.tmc.identityuser.dto.request.StatusRequestDTO;
import com.tmc.identityuser.repository.IdentityStore;
import com.tmc.identityuser.service.IdentityService;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final IdentityService identityService;
    private final IdentityStore identityStore;

    public DeviceController(IdentityService identityService, IdentityStore identityStore) {
        this.identityService = identityService;
        this.identityStore = identityStore;
    }

    @PostMapping
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    DeviceIdentityDTO registerDevice(@Valid @RequestBody DeviceRequestDTO request, Authentication authentication) {
        identityService.registerDevice(request.clientId(), request.secret(), request.stationId(), authentication.getName());
        return identityStore.device(request.clientId()).orElseThrow();
    }

    @PutMapping("/{clientId}/status")
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    DeviceIdentityDTO deviceStatus(@PathVariable String clientId, @Valid @RequestBody StatusRequestDTO request,
            Authentication authentication) {
        identityService.setDeviceActive(clientId, request.active(), authentication.getName());
        return identityStore.device(clientId).orElseThrow();
    }
}
