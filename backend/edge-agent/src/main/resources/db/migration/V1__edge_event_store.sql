CREATE SCHEMA IF NOT EXISTS aas_edge;

-- The edge event store is the system of record for scans performed at this factory.
-- Central receives an append-only replica; it never overwrites these rows.
CREATE TABLE aas_edge.access_event (
    event_id            uuid         PRIMARY KEY,
    scan_id             uuid         NOT NULL,
    station_id          varchar(32)  NOT NULL,
    employee_ref        varchar(64),
    direction           varchar(8)   NOT NULL CHECK (direction IN ('IN', 'OUT')),
    decision            varchar(16)  NOT NULL CHECK (decision IN ('ALLOWED', 'DENIED')),
    reason              varchar(128),
    qr_key_id           varchar(64),
    payload_fingerprint char(64)     NOT NULL,
    received_at         timestamptz  NOT NULL,
    source_time         timestamptz,
    schema_version      integer      NOT NULL DEFAULT 1,
    CONSTRAINT uq_access_event_scan UNIQUE (scan_id)
);

CREATE INDEX idx_access_event_received_at ON aas_edge.access_event (received_at);

CREATE TABLE aas_edge.gate_intent (
    command_id  uuid         PRIMARY KEY,
    scan_id     uuid         NOT NULL REFERENCES aas_edge.access_event (scan_id),
    gate_id     varchar(64)  NOT NULL,
    action      varchar(16)  NOT NULL,
    state       varchar(24)  NOT NULL CHECK (state IN (
                    'PENDING', 'SENT', 'CONFIRMED', 'REJECTED', 'FAILED', 'UNKNOWN')),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE aas_edge.outbox (
    event_id        uuid         PRIMARY KEY,
    aggregate_type  varchar(40)  NOT NULL,
    aggregate_id    varchar(64)  NOT NULL,
    event_type      varchar(80)  NOT NULL,
    payload         jsonb        NOT NULL,
    schema_version  integer      NOT NULL DEFAULT 1,
    status          varchar(16)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts        integer      NOT NULL DEFAULT 0,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    next_attempt_at timestamptz,
    sent_at         timestamptz
);

CREATE INDEX idx_outbox_pending
    ON aas_edge.outbox (status, next_attempt_at, created_at);
