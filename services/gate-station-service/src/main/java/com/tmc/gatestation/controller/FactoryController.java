package com.tmc.gatestation.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.gatestation.dto.request.FactoryRequestDTO;
import com.tmc.gatestation.dto.response.FactoryResponseDTO;
import com.tmc.gatestation.dto.request.FactoryStatusRequestDTO;
import com.tmc.gatestation.service.StationService;

@RestController
@RequestMapping("/factories")
public class FactoryController {

    private final StationService stationService;

    public FactoryController(StationService stationService) {
        this.stationService = stationService;
    }

    @PostMapping
    FactoryResponseDTO createFactory(@Valid @RequestBody FactoryRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        UUID factoryId = UUID.randomUUID();
        stationService.createFactory(factoryId, request.code(), request.name(), jwt.getSubject());
        return new FactoryResponseDTO(factoryId);
    }

    @PutMapping("/{factoryId}/status")
    void factoryStatus(@PathVariable UUID factoryId, @Valid @RequestBody FactoryStatusRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        stationService.setFactoryActive(factoryId, request.active(), jwt.getSubject());
    }
}
