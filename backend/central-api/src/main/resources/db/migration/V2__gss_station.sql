CREATE TABLE gss.factory (
    factory_id   varchar(32)  PRIMARY KEY,
    name         varchar(128) NOT NULL,
    timezone     varchar(64)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE gss.gate_station (
    station_id      varchar(32)  PRIMARY KEY,
    factory_id      varchar(32)  NOT NULL REFERENCES gss.factory (factory_id),
    name            varchar(128) NOT NULL,
    direction       varchar(8)   NOT NULL CHECK (direction IN ('IN', 'OUT')),
    gate_id         varchar(64)  NOT NULL,
    kiosk_id        varchar(64)  NOT NULL,
    status          varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    config_revision integer      NOT NULL DEFAULT 1 CHECK (config_revision > 0),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_gate_station_gate_direction UNIQUE (factory_id, gate_id, direction),
    CONSTRAINT uq_gate_station_kiosk UNIQUE (kiosk_id)
);

CREATE INDEX idx_gate_station_factory ON gss.gate_station (factory_id);

COMMENT ON TABLE gss.gate_station IS
    'Gate station configuration. Events store the config_revision in force at scan time.';
