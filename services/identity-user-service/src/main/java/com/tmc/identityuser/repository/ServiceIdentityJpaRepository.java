package com.tmc.identityuser.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tmc.identityuser.entity.ServiceIdentityEntity;

public interface ServiceIdentityJpaRepository extends JpaRepository<ServiceIdentityEntity, String> {
    boolean existsByClientIdAndActiveFalse(String clientId);
}
