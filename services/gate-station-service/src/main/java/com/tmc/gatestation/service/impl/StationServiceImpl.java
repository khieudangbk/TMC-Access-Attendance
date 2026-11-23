package com.tmc.gatestation.service.impl;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.gatestation.dto.response.StationCheckDTO;
import com.tmc.gatestation.entity.Direction;
import com.tmc.gatestation.entity.Station;
import com.tmc.gatestation.repository.StationRepository;
import com.tmc.gatestation.service.StationService;

@Service("stationService")
public class StationServiceImpl implements StationService {

    private final StationRepository repository;

    public StationServiceImpl(StationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public void createFactory(UUID factoryId, String code, String name, String actor) {
        repository.insertFactory(factoryId, code, name);
        audit(actor, factoryId, "FACTORY_CREATED");
    }

    @Transactional
    @Override
    public Station createStation(UUID factoryId, String code, String deviceId, Direction direction,
            String controllerReference, String actor) {
        UUID stationId = UUID.randomUUID();
        repository.insertStation(stationId, factoryId, code, deviceId, direction, controllerReference);
        audit(actor, stationId, "STATION_CREATED");
        return station(stationId);
    }

    @Transactional(readOnly = true)
    @Override
    public Station station(UUID stationId) {
        return repository.findStation(stationId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    @Override
    public StationCheckDTO verify(UUID stationId, String deviceId) {
        Station station = station(stationId);
        boolean allowed = station.active() && station.factoryActive() && station.deviceId().equals(deviceId);
        return new StationCheckDTO(stationId, station.factoryId(), station.direction(), allowed,
                !station.active() || !station.factoryActive() ? "STATION_DISABLED"
                        : allowed ? "ALLOWED" : "DEVICE_STATION_MISMATCH", station.revision());
    }

    @Transactional
    @Override
    public Station setActive(UUID stationId, boolean active, long expectedRevision, String actor) {
        if (repository.setActive(active, stationId, expectedRevision) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STATION_REVISION_CONFLICT");
        }
        audit(actor, stationId, active ? "STATION_ENABLED" : "STATION_DISABLED");
        return station(stationId);
    }

    @Transactional
    @Override
    public void setFactoryActive(UUID factoryId, boolean active, String actor) {
        if (repository.setFactoryActive(active, factoryId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FACTORY_NOT_FOUND");
        }
        audit(actor, factoryId, active ? "FACTORY_ENABLED" : "FACTORY_DISABLED");
    }

    @Transactional
    @Override
    public void heartbeat(UUID stationId, String deviceId) {
        if (!verify(stationId, deviceId).allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_STATION_DENIED");
        }
        repository.heartbeat(stationId);
    }

    private void audit(String actor, UUID targetId, String eventType) {
        repository.audit(actor, targetId, eventType);
    }
}
