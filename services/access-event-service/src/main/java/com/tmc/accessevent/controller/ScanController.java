package com.tmc.accessevent.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.dto.request.ScanRequestDTO;
import com.tmc.accessevent.service.AccessService;
import com.tmc.accessevent.utils.DeviceIdentityUtils;

@RestController
@RequestMapping("/scans")
public class ScanController {

    private final AccessService accessService;

    public ScanController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping
    ScanDecisionDTO scan(@Valid @RequestBody ScanRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        if (!DeviceIdentityUtils.stationId(jwt).equals(request.stationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "STATION_MISMATCH");
        }
        return accessService.scan(request.scanId(), request.stationId(), jwt.getSubject(), request.qrToken());
    }
}
