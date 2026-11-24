package com.tmc.gatestation.service;

import java.util.UUID;

import com.tmc.gatestation.dto.response.StationCheckDTO;
import com.tmc.gatestation.entity.Direction;
import com.tmc.gatestation.entity.Station;

public interface StationService {

    void createFactory(UUID factoryId, String code, String name, String actor);

    Station createStation(UUID factoryId, String code, String deviceId, Direction direction,
            String controllerReference, String actor);

    Station station(UUID stationId);

    StationCheckDTO verify(UUID stationId, String deviceId);

    Station setActive(UUID stationId, boolean active, long expectedRevision, String actor);

    void setFactoryActive(UUID factoryId, boolean active, String actor);

    void heartbeat(UUID stationId, String deviceId);
}
