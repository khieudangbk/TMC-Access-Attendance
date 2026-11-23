package com.tmc.accessevent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tmc.accessevent.entity.GateCommandEntity;

public interface GateCommandRepository extends JpaRepository<GateCommandEntity, UUID> {
    @Query(value = "SELECT * FROM gate_commands WHERE command_id = :commandId FOR UPDATE", nativeQuery = true)
    Optional<GateCommandEntity> findLocked(UUID commandId);
}
