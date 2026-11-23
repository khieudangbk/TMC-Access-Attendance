package com.tmc.identityuser.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tmc.identityuser.entity.DeviceIdentityEntity;

public interface DeviceIdentityJpaRepository extends JpaRepository<DeviceIdentityEntity, String> {
}
