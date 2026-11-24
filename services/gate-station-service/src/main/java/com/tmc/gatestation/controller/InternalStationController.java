package com.tmc.gatestation.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.gatestation.dto.request.CheckRequestDTO;
import com.tmc.gatestation.dto.response.StationCheckDTO;
import com.tmc.gatestation.service.StationService;

@RestController
@RequestMapping("/internal/stations")
public class InternalStationController {

    private final StationService stationService;

    public InternalStationController(StationService stationService) {
        this.stationService = stationService;
    }

    @PostMapping("/{stationId}/checks")
    StationCheckDTO verify(@PathVariable UUID stationId, @Valid @RequestBody CheckRequestDTO request) {
        return stationService.verify(stationId, request.deviceId());
    }
}
