package com.tmc.identityuser.service;

import java.util.Set;
import java.util.UUID;

import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.entity.Employee.Role;

public interface IdentityService {

    Employee createEmployee(String employeeCode, String fullName, String username,
            String password, Set<Role> roles, Set<UUID> factoryIds, String actor);

    Employee updatePermissions(UUID employeeId, Set<Role> roles, Set<UUID> factoryIds, String actor);

    Employee updateProfile(UUID employeeId, String fullName, String actor);

    Employee setActive(UUID employeeId, boolean active, String actor);

    void registerDevice(String clientId, String secret, UUID stationId, String actor);

    void setDeviceActive(String clientId, boolean active, String actor);

    void registerServiceClient(String clientId, String secret, Set<String> scopes, String actor);

    void setServiceActive(String clientId, boolean active, String actor);

    void bootstrap(String username, String password);
}
