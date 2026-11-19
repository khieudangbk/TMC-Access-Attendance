package com.tmc.identityuser.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.entity.EmployeeEntity;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {
    Optional<EmployeeEntity> findByUsername(String username);

    @Query("select count(employee) from EmployeeEntity employee join employee.roles role where employee.active = true and role = :role")
    long countActiveByRole(Role role);

    @Query("select employee.employeeId from EmployeeEntity employee where :factoryId member of employee.factoryIds order by employee.employeeId")
    List<UUID> factoryEmployees(UUID factoryId);
}
