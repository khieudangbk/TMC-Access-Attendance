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

import com.tmc.identityuser.dto.request.ServiceClientRequestDTO;
import com.tmc.identityuser.dto.request.StatusRequestDTO;
import com.tmc.identityuser.service.IdentityService;

@RestController
@RequestMapping("/service-clients")
public class ServiceClientController {

    private final IdentityService identityService;

    public ServiceClientController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    void serviceClient(@Valid @RequestBody ServiceClientRequestDTO request, Authentication authentication) {
        identityService.registerServiceClient(request.clientId(), request.secret(), request.scopes(), authentication.getName());
    }

    @PutMapping("/{clientId}/status")
    @PreAuthorize("@identityAccess.isAdmin(authentication)")
    void serviceClientStatus(@PathVariable String clientId, @Valid @RequestBody StatusRequestDTO request,
            Authentication authentication) {
        identityService.setServiceActive(clientId, request.active(), authentication.getName());
    }
}
