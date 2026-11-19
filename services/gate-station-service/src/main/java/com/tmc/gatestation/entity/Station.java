package com.tmc.gatestation.entity;

import java.util.UUID;

public record Station(UUID stationId, UUID factoryId, String stationCode, String deviceId,
        Direction direction, String controllerReference, boolean active, boolean factoryActive, long revision) {
}
