package com.tmc.gatestation.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.gatestation.dto.request.StationRequestDTO;
import com.tmc.gatestation.dto.request.StatusRequestDTO;
import com.tmc.gatestation.entity.Station;
import com.tmc.gatestation.service.StationService;

@RestController
@RequestMapping("/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @PostMapping
    Station createStation(@Valid @RequestBody StationRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        return stationService.createStation(request.factoryId(), request.code(), request.deviceId(),
                request.direction(), request.controllerReference(), jwt.getSubject());
    }

    @GetMapping("/{stationId}")
    Station station(@PathVariable UUID stationId) {
        return stationService.station(stationId);
    }

    @PutMapping("/{stationId}/status")
    Station stationStatus(@PathVariable UUID stationId, @Valid @RequestBody StatusRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        return stationService.setActive(stationId, request.active(), request.expectedRevision(), jwt.getSubject());
    }

    @PostMapping("/{stationId}/heartbeat")
    void heartbeat(@PathVariable UUID stationId, @AuthenticationPrincipal Jwt jwt) {
        if (!stationId.toString().equals(jwt.getClaimAsString("station_id"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "STATION_MISMATCH");
        }
        stationService.heartbeat(stationId, jwt.getSubject());
    }
}
