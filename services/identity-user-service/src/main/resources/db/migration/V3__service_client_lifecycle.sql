CREATE TABLE service_identities (
    client_id VARCHAR(100) PRIMARY KEY REFERENCES oauth2_registered_client(client_id),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
