package com.tmc.gatestation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tmc.gatestation.entity.FactoryEntity;

public interface FactoryJpaRepository extends JpaRepository<FactoryEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FactoryEntity factory set factory.active = :active where factory.factoryId = :factoryId")
    int setActive(boolean active, UUID factoryId);
}
