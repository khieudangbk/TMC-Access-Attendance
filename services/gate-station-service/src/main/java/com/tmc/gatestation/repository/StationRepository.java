package com.tmc.gatestation.repository;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.tmc.gatestation.entity.Direction;
import com.tmc.gatestation.entity.FactoryEntity;
import com.tmc.gatestation.entity.Station;
import com.tmc.gatestation.entity.StationEntity;

@Repository
public class StationRepository {
    private final EntityManager entityManager;
    private final FactoryJpaRepository factories;
    private final StationJpaRepository stations;

    public StationRepository(EntityManager entityManager, FactoryJpaRepository factories, StationJpaRepository stations) {
        this.entityManager = entityManager;
        this.factories = factories;
        this.stations = stations;
    }

    public int insertFactory(UUID factoryId, String code, String name) {
        entityManager.persist(new FactoryEntity(factoryId, code, name, true));
        entityManager.flush();
        return 1;
    }

    public int insertStation(UUID stationId, UUID factoryId, String code, String deviceId, Direction direction,
            String controllerReference) {
        entityManager.persist(new StationEntity(stationId, factoryId, code, deviceId, direction, controllerReference,
                true, 1, null));
        entityManager.flush();
        return 1;
    }

    public List<Station> findStation(UUID stationId) {
        return stations.station(stationId);
    }

    public int setActive(boolean active, UUID stationId, long expectedRevision) {
        return stations.setActive(active, stationId, expectedRevision);
    }

    public int setFactoryActive(boolean active, UUID factoryId) {
        return factories.setActive(active, factoryId);
    }

    public int heartbeat(UUID stationId) {
        return stations.heartbeat(stationId);
    }

    public int audit(String actor, UUID targetId, String eventType) {
        return entityManager.createNativeQuery("""
                INSERT INTO station_audit_events (event_id, actor_id, target_id, event_type) VALUES (?1, ?2, ?3, ?4)
                """).setParameter(1, UUID.randomUUID()).setParameter(2, actor)
                .setParameter(3, targetId).setParameter(4, eventType).executeUpdate();
    }
}
