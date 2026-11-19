CREATE TABLE integration_outbox (
    event_id UUID PRIMARY KEY,
    routing_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
CREATE INDEX integration_outbox_pending ON integration_outbox(created_at) WHERE published_at IS NULL;
CREATE FUNCTION enqueue_audit_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO integration_outbox(event_id, routing_key, payload) VALUES (NEW.event_id, 'audit.qr-service',
        jsonb_build_object('eventId', NEW.event_id, 'eventType', 'AUDIT_RECORDED', 'source', 'qr-service',
            'actor', NEW.employee_id::text, 'entityId', coalesce(NEW.qr_id::text, NEW.employee_id::text), 'action', NEW.event_type, 'factoryId', (SELECT factory_id FROM qr_credentials WHERE qr_id = NEW.qr_id),
            'occurredAt', NEW.occurred_at, 'version', 1));
    RETURN NEW;
END; $$;
CREATE TRIGGER audit_outbox AFTER INSERT ON qr_audit_events FOR EACH ROW EXECUTE FUNCTION enqueue_audit_event();
INSERT INTO integration_outbox(event_id, routing_key, payload)
SELECT evidence.event_id, 'audit.qr-service',
    jsonb_build_object('eventId', evidence.event_id, 'eventType', 'AUDIT_RECORDED',
        'source', 'qr-service', 'actor', evidence.employee_id::text,
        'entityId', coalesce(evidence.qr_id::text, evidence.employee_id::text), 'action', evidence.event_type,
        'factoryId', (SELECT factory_id FROM qr_credentials WHERE qr_id = evidence.qr_id), 'occurredAt', evidence.occurred_at, 'version', 1)
FROM qr_audit_events evidence ON CONFLICT DO NOTHING;

CREATE FUNCTION protect_integration_outbox() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (to_jsonb(OLD) - 'published_at') IS DISTINCT FROM (to_jsonb(NEW) - 'published_at') THEN
        RAISE EXCEPTION 'Outbox evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER integration_outbox_immutable BEFORE UPDATE OR DELETE ON integration_outbox
    FOR EACH ROW EXECUTE FUNCTION protect_integration_outbox();
