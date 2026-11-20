package com.tmc.identityuser.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.repository.IdentityStore;
import com.tmc.identityuser.service.IdentityService;

@Service("identityService")
public class IdentityServiceImpl implements IdentityService {

    private final IdentityStore identityStore;
    private final PasswordEncoder passwordEncoder;
    private final RegisteredClientRepository clients;

    public IdentityServiceImpl(IdentityStore identityStore, PasswordEncoder passwordEncoder, RegisteredClientRepository clients) {
        this.identityStore = identityStore;
        this.passwordEncoder = passwordEncoder;
        this.clients = clients;
    }

    @Transactional
    @Override
    public Employee createEmployee(String employeeCode, String fullName, String username,
            String password, Set<Role> roles, Set<UUID> factoryIds, String actor) {
        validateSecret(password);
        var employee = new Employee(UUID.randomUUID(), employeeCode, fullName, true, Set.copyOf(roles), Set.copyOf(factoryIds));
        identityStore.create(employee, username, passwordEncoder.encode(password));
        identityStore.audit(actor, "EMPLOYEE_CREATED", employee.employeeId().toString());
        return employee;
    }

    @Transactional
    @Override
    public Employee updatePermissions(UUID employeeId, Set<Role> roles, Set<UUID> factoryIds, String actor) {
        identityStore.lockAdministration();
        Employee employee = identityStore.employee(employeeId);
        preventLastAdminRemoval(employee, roles.contains(Role.ADMIN) && employee.active());
        identityStore.replacePermissions(employeeId, roles, factoryIds);
        identityStore.audit(actor, "PERMISSIONS_CHANGED", employeeId.toString());
        return identityStore.employee(employeeId);
    }

    @Transactional
    @Override
    public Employee updateProfile(UUID employeeId, String fullName, String actor) {
        identityStore.employee(employeeId);
        identityStore.updateFullName(employeeId, fullName);
        identityStore.audit(actor, "EMPLOYEE_PROFILE_CHANGED", employeeId.toString());
        return identityStore.employee(employeeId);
    }

    @Transactional
    @Override
    public Employee setActive(UUID employeeId, boolean active, String actor) {
        identityStore.lockAdministration();
        Employee employee = identityStore.employee(employeeId);
        preventLastAdminRemoval(employee, active && employee.roles().contains(Role.ADMIN));
        identityStore.setActive(employeeId, active);
        identityStore.audit(actor, active ? "EMPLOYEE_ENABLED" : "EMPLOYEE_DISABLED", employeeId.toString());
        return identityStore.employee(employeeId);
    }

    @Transactional
    @Override
    public void registerDevice(String clientId, String secret, UUID stationId, String actor) {
        validateSecret(secret);
        clients.save(RegisteredClient.withId(UUID.randomUUID().toString()).clientId(clientId)
                .clientSecret(passwordEncoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("access.scan").scope("access.confirm")
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofSeconds(60)).build()).build());
        identityStore.addDevice(clientId, stationId);
        identityStore.audit(actor, "DEVICE_REGISTERED", clientId);
    }

    @Transactional
    @Override
    public void setDeviceActive(String clientId, boolean active, String actor) {
        identityStore.setDeviceActive(clientId, active);
        identityStore.audit(actor, active ? "DEVICE_ENABLED" : "DEVICE_DISABLED", clientId);
    }

    @Transactional
    @Override
    public void registerServiceClient(String clientId, String secret, Set<String> scopes, String actor) {
        validateSecret(secret);
        if (scopes.isEmpty() || !Set.of("identity.verify", "qr.verify", "stations.verify", "stations.manage")
                .containsAll(scopes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SERVICE_SCOPE");
        }
        var builder = RegisteredClient.withId(UUID.randomUUID().toString()).clientId(clientId)
                .clientSecret(passwordEncoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofSeconds(60)).build());
        scopes.forEach(builder::scope);
        clients.save(builder.build());
        identityStore.addServiceClient(clientId);
        identityStore.audit(actor, "SERVICE_CLIENT_REGISTERED", clientId);
    }

    @Transactional
    @Override
    public void setServiceActive(String clientId, boolean active, String actor) {
        identityStore.setServiceActive(clientId, active);
        identityStore.audit(actor, active ? "SERVICE_CLIENT_ENABLED" : "SERVICE_CLIENT_DISABLED", clientId);
    }

    @Transactional
    @Override
    public void bootstrap(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }
        identityStore.lockAdministration();
        if (identityStore.accountCount() == 0) {
            createEmployee("SYSTEM-ADMIN", "System Administrator", username, password,
                    Set.of(Role.ADMIN), Set.of(), "bootstrap");
        }
    }

    private void preventLastAdminRemoval(Employee employee, boolean remainsAdmin) {
        if (employee.active() && employee.roles().contains(Role.ADMIN) && !remainsAdmin
                && identityStore.activeAdminCount() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LAST_ADMIN_REQUIRED");
        }
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.length() < 12 || secret.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SECRET_LENGTH_INVALID");
        }
    }
}
