CREATE TABLE employees (
    employee_id UUID PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE employee_roles (
    employee_id UUID NOT NULL REFERENCES employees(employee_id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('EMPLOYEE', 'SUPERVISOR', 'HR', 'SECURITY', 'ADMIN', 'EXECUTIVE')),
    PRIMARY KEY (employee_id, role)
);
CREATE TABLE factory_access_grants (
    employee_id UUID NOT NULL REFERENCES employees(employee_id),
    factory_id UUID NOT NULL,
    PRIMARY KEY (employee_id, factory_id)
);
CREATE TABLE device_identities (
    client_id VARCHAR(100) PRIMARY KEY REFERENCES oauth2_registered_client(client_id),
    station_id UUID NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE identity_audit_events (
    event_id UUID PRIMARY KEY,
    actor_id VARCHAR(200) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(200) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
CREATE FUNCTION reject_identity_audit_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR OLD.actor_id IS DISTINCT FROM NEW.actor_id
        OR OLD.event_type IS DISTINCT FROM NEW.event_type OR OLD.target_id IS DISTINCT FROM NEW.target_id
        OR OLD.event_id IS DISTINCT FROM NEW.event_id OR OLD.occurred_at IS DISTINCT FROM NEW.occurred_at THEN
        RAISE EXCEPTION 'identity audit evidence is immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER identity_audit_immutable BEFORE UPDATE OR DELETE ON identity_audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_identity_audit_mutation();
