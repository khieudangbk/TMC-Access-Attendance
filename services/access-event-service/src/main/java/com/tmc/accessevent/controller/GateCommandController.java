package com.tmc.accessevent.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.accessevent.dto.request.ConfirmationRequestDTO;
import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.service.AccessService;
import com.tmc.accessevent.utils.DeviceIdentityUtils;

@RestController
@RequestMapping("/commands")
public class GateCommandController {

    private final AccessService accessService;

    public GateCommandController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/{commandId}/confirmations")
    ConfirmationResultDTO confirm(@PathVariable UUID commandId,
            @Valid @RequestBody ConfirmationRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        return accessService.confirm(commandId, DeviceIdentityUtils.stationId(jwt),
                request.confirmationId(), request.outcome(), request.occurredAt());
    }
}
