package com.tmc.accessevent.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tmc.accessevent.entity.AccessEventEntity;

public interface AccessEventRepository extends JpaRepository<AccessEventEntity, UUID> {
    @Query("select event.occurredAt from AccessEventEntity event where event.scanId = :scanId and event.eventType = 'GATE_OPENED'")
    Optional<Instant> openedAt(UUID scanId);
}
