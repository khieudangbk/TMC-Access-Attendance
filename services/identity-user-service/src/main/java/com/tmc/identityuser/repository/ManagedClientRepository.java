package com.tmc.identityuser.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ManagedClientRepository implements RegisteredClientRepository {

    private final JdbcRegisteredClientRepository delegate;
    private final IdentityStore identityStore;

    public ManagedClientRepository(JdbcTemplate jdbcTemplate, IdentityStore identityStore) {
        this.delegate = new JdbcRegisteredClientRepository(jdbcTemplate);
        this.identityStore = identityStore;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (identityStore.device(clientId).filter(device -> !device.active()).isPresent()
                || identityStore.serviceDisabled(clientId)) {
            return null;
        }
        return delegate.findByClientId(clientId);
    }
}
