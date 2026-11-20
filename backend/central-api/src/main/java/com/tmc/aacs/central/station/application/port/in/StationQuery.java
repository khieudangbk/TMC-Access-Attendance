package com.tmc.aacs.central.station.application.port.in;

import java.util.List;

import com.tmc.aacs.central.station.domain.GateStation;

public interface StationQuery {

    List<GateStation> findAll();

    GateStation getById(String stationId);
}
