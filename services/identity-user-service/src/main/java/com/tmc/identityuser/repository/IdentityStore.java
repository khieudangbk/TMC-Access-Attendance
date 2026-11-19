package com.tmc.identityuser.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.identityuser.dto.response.DeviceIdentityDTO;
import com.tmc.identityuser.entity.DeviceIdentityEntity;
import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.entity.EmployeeEntity;
import com.tmc.identityuser.entity.ServiceIdentityEntity;

@Repository
@Transactional(readOnly = true)
public class IdentityStore {
    private final EntityManager entityManager;
    private final EmployeeJpaRepository employees;
    private final DeviceIdentityJpaRepository devices;
    private final ServiceIdentityJpaRepository services;

    public IdentityStore(EntityManager entityManager, EmployeeJpaRepository employees,
            DeviceIdentityJpaRepository devices, ServiceIdentityJpaRepository services) {
        this.entityManager = entityManager;
        this.employees = employees;
        this.devices = devices;
        this.services = services;
    }

    public Employee employee(UUID employeeId) {
        return model(requiredEmployee(employeeId));
    }

    public Set<Role> roles(UUID employeeId) {
        return employees.findById(employeeId).map(employee -> new HashSet<>(employee.getRoles())).orElseGet(HashSet::new);
    }

    public List<Employee> employees(int limit, int offset) {
        return entityManager.createQuery("select employee from EmployeeEntity employee order by employee.employeeCode", EmployeeEntity.class)
                .setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::model).toList();
    }

    public List<UUID> factoryEmployees(UUID factoryId) {
        return employees.factoryEmployees(factoryId);
    }

    @Transactional
    public void updateFullName(UUID employeeId, String fullName) {
        employees.findById(employeeId).ifPresent(employee -> employee.setFullName(fullName));
    }

    public Set<UUID> factories(UUID employeeId) {
        return employees.findById(employeeId).map(employee -> new HashSet<>(employee.getFactoryIds())).orElseGet(HashSet::new);
    }

    public UserDetails userDetails(String username) {
        EmployeeEntity employee = employees.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
        String[] authorities = employee.getRoles().stream().map(role -> "ROLE_" + role.name()).toArray(String[]::new);
        return User.withUsername(employee.getEmployeeId().toString()).password(employee.getPasswordHash())
                .disabled(!employee.isActive()).authorities(authorities).build();
    }

    @Transactional
    public void create(Employee employee, String username, String passwordHash) {
        entityManager.persist(new EmployeeEntity(employee.employeeId(), employee.employeeCode(), employee.fullName(),
                username.trim().toLowerCase(Locale.ROOT), passwordHash, employee.active(),
                new HashSet<>(employee.roles()), new HashSet<>(employee.factoryIds())));
        entityManager.flush();
    }

    @Transactional
    public void replacePermissions(UUID employeeId, Set<Role> roles, Set<UUID> factoryIds) {
        EmployeeEntity employee = requiredEmployee(employeeId);
        employee.getRoles().clear();
        employee.getRoles().addAll(roles);
        employee.getFactoryIds().clear();
        employee.getFactoryIds().addAll(factoryIds);
        entityManager.flush();
    }

    @Transactional
    public void setActive(UUID employeeId, boolean active) {
        employees.findById(employeeId).ifPresent(employee -> employee.setActive(active));
    }

    @Transactional
    public void lockAdministration() {
        entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(772001)").getSingleResult();
    }

    public int accountCount() {
        return Math.toIntExact(employees.count());
    }

    public int activeAdminCount() {
        return Math.toIntExact(employees.countActiveByRole(Role.ADMIN));
    }

    @Transactional
    public void addDevice(String clientId, UUID stationId) {
        entityManager.persist(new DeviceIdentityEntity(clientId, stationId, true));
        entityManager.flush();
    }

    public Optional<DeviceIdentityDTO> device(String clientId) {
        return devices.findById(clientId).map(device -> new DeviceIdentityDTO(device.getClientId(), device.getStationId(), device.isActive()));
    }

    @Transactional
    public void setDeviceActive(String clientId, boolean active) {
        devices.findById(clientId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND"))
                .setActive(active);
    }

    @Transactional
    public void addServiceClient(String clientId) {
        entityManager.persist(new ServiceIdentityEntity(clientId, true));
        entityManager.flush();
    }

    public boolean serviceDisabled(String clientId) {
        return services.existsByClientIdAndActiveFalse(clientId);
    }

    @Transactional
    public void setServiceActive(String clientId, boolean active) {
        services.findById(clientId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SERVICE_CLIENT_NOT_FOUND"))
                .setActive(active);
    }

    @Transactional
    public void audit(String actor, String eventType, String target) {
        entityManager.createNativeQuery("""
                INSERT INTO identity_audit_events (event_id, actor_id, event_type, target_id) VALUES (?1, ?2, ?3, ?4)
                """).setParameter(1, UUID.randomUUID()).setParameter(2, actor).setParameter(3, eventType)
                .setParameter(4, target).executeUpdate();
    }

    private EmployeeEntity requiredEmployee(UUID employeeId) {
        return employees.findById(employeeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
    }

    private Employee model(EmployeeEntity employee) {
        return new Employee(employee.getEmployeeId(), employee.getEmployeeCode(), employee.getFullName(), employee.isActive(),
                new HashSet<>(employee.getRoles()), new HashSet<>(employee.getFactoryIds()));
    }
}
