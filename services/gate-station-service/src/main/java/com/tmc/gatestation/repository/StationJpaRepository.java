package com.tmc.gatestation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.gatestation.entity.Station;
import com.tmc.gatestation.entity.StationEntity;

public interface StationJpaRepository extends JpaRepository<StationEntity, UUID> {

    @Query("""
            select new com.tmc.gatestation.entity.Station(station.stationId, station.factoryId,
                station.stationCode, station.deviceId, station.direction, station.controllerReference,
                station.active, factory.active, station.revision)
            from StationEntity station join FactoryEntity factory on factory.factoryId = station.factoryId
            where station.stationId = :stationId
            """)
    List<Station> station(UUID stationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update StationEntity station set station.active = :active, station.revision = station.revision + 1
            where station.stationId = :stationId and station.revision = :expectedRevision
            """)
    int setActive(boolean active, UUID stationId, long expectedRevision);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StationEntity station set station.lastSeenAt = CURRENT_INSTANT where station.stationId = :stationId")
    int heartbeat(UUID stationId);
}
