package com.tmc.aacs.central.station.domain;

public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(String stationId) {
        super("Gate station not found: " + stationId);
    }
}
