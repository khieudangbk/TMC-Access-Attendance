package com.tmc.identityuser.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import com.tmc.identityuser.entity.Employee.Role;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class EmployeeEntity {
    @Id
    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Column(name = "active")
    private boolean active;

    @ElementCollection
    @CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Set<Role> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "factory_access_grants", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "factory_id")
    private Set<UUID> factoryIds = new HashSet<>();
}
