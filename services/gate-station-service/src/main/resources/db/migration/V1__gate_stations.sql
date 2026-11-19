CREATE TABLE factories (
    factory_id UUID PRIMARY KEY,
    factory_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE TABLE stations (
    station_id UUID PRIMARY KEY,
    factory_id UUID NOT NULL REFERENCES factories(factory_id),
    station_code VARCHAR(50) NOT NULL UNIQUE,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    direction VARCHAR(5) NOT NULL CHECK (direction IN ('ENTRY', 'EXIT')),
    controller_reference VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    last_seen_at TIMESTAMPTZ
);
CREATE TABLE station_audit_events (
    event_id UUID PRIMARY KEY,
    actor_id VARCHAR(200) NOT NULL,
    target_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
