package com.tmc.aacs.central.station.domain;

/**
 * A physical gate station. A station has exactly one direction and maps to one turnstile gate.
 * Configuration changes must bump {@code configRevision}; access events keep the revision in force
 * at scan time.
 */
public record GateStation(
        String stationId,
        String factoryId,
        String factoryName,
        String name,
        Direction direction,
        String gateId,
        String kioskId,
        StationStatus status,
        int configRevision
) {
}
