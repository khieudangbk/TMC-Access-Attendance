package com.tmc.aacs.central.station.adapter.in.web;

import java.time.Instant;
import java.util.List;

import com.tmc.aacs.central.station.application.port.in.StationQuery;
import com.tmc.aacs.central.station.domain.GateStation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationQuery stationQuery;

    public StationController(StationQuery stationQuery) {
        this.stationQuery = stationQuery;
    }

    @GetMapping
    public StationListResponse list() {
        List<StationResponse> stations = stationQuery.findAll().stream()
                .map(StationResponse::from)
                .toList();
        return new StationListResponse(stations, stations.size(), Instant.now());
    }

    @GetMapping("/{stationId}")
    public StationResponse get(@PathVariable String stationId) {
        return StationResponse.from(stationQuery.getById(stationId));
    }

    public record StationListResponse(List<StationResponse> items, int total, Instant generatedAt) {
    }

    public record StationResponse(
            String stationId,
            String factoryId,
            String factoryName,
            String name,
            String direction,
            String gateId,
            String kioskId,
            String status,
            int configRevision
    ) {

        static StationResponse from(GateStation station) {
            return new StationResponse(
                    station.stationId(),
                    station.factoryId(),
                    station.factoryName(),
                    station.name(),
                    station.direction().name(),
                    station.gateId(),
                    station.kioskId(),
                    station.status().name(),
                    station.configRevision());
        }
    }
}
