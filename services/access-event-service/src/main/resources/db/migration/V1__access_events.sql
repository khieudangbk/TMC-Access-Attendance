CREATE TABLE scan_requests (
    scan_id UUID PRIMARY KEY,
    station_id UUID NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    employee_id UUID,
    factory_id UUID,
    direction VARCHAR(10) CHECK (direction IN ('ENTRY', 'EXIT')),
    allowed BOOLEAN,
    reason VARCHAR(80),
    command_id UUID UNIQUE,
    command_expires_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE qr_redemptions (
    qr_id UUID PRIMARY KEY,
    scan_id UUID NOT NULL UNIQUE REFERENCES scan_requests(scan_id),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE gate_commands (
    command_id UUID PRIMARY KEY,
    scan_id UUID NOT NULL UNIQUE REFERENCES scan_requests(scan_id),
    station_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    factory_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('ENTRY', 'EXIT')),
    expires_at TIMESTAMPTZ NOT NULL,
    outcome VARCHAR(12) NOT NULL CHECK (outcome IN ('PENDING', 'OPENED', 'FAILED', 'UNKNOWN', 'PASSED'))
);

CREATE TABLE command_confirmations (
    confirmation_id UUID PRIMARY KEY,
    command_id UUID NOT NULL REFERENCES gate_commands(command_id),
    fingerprint CHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE access_events (
    event_id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scan_requests(scan_id),
    station_id UUID NOT NULL,
    employee_id UUID,
    factory_id UUID,
    direction VARCHAR(10) CHECK (direction IN ('ENTRY', 'EXIT')),
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN
        ('ACCESS_ALLOWED', 'ACCESS_DENIED', 'GATE_OPENED', 'GATE_FAILED', 'GATE_UNKNOWN', 'PASSAGE_CONFIRMED')),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (scan_id, event_type)
);
CREATE INDEX access_events_employee_time ON access_events(employee_id, occurred_at);
CREATE INDEX access_events_factory_time ON access_events(factory_id, occurred_at);

CREATE FUNCTION reject_access_event_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'access_events is append-only';
END;
$$;
CREATE TRIGGER access_events_append_only BEFORE UPDATE OR DELETE ON access_events
    FOR EACH ROW EXECUTE FUNCTION reject_access_event_mutation();

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY REFERENCES access_events(event_id),
    event_type VARCHAR(30) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX outbox_events_pending ON outbox_events(created_at) WHERE published_at IS NULL;
