CREATE TABLE qr_credentials (
    qr_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    factory_id UUID NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    key_id VARCHAR(100) NOT NULL,
    revoked_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at)
);
CREATE INDEX qr_employee_factory ON qr_credentials(employee_id, factory_id);
CREATE TABLE qr_audit_events (
    event_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    qr_id UUID,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
