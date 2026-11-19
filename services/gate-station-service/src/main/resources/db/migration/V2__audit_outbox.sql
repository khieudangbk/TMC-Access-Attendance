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
    INSERT INTO integration_outbox(event_id, routing_key, payload) VALUES (NEW.event_id, 'audit.gate-station-service',
        jsonb_build_object('eventId', NEW.event_id, 'eventType', 'AUDIT_RECORDED', 'source', 'gate-station-service',
            'actor', NEW.actor_id, 'entityId', NEW.target_id::text, 'action', NEW.event_type, 'factoryId', coalesce((SELECT factory_id FROM stations WHERE station_id = NEW.target_id), (SELECT factory_id FROM factories WHERE factory_id = NEW.target_id)),
            'occurredAt', NEW.occurred_at, 'version', 1));
    RETURN NEW;
END; $$;
CREATE TRIGGER audit_outbox AFTER INSERT ON station_audit_events FOR EACH ROW EXECUTE FUNCTION enqueue_audit_event();
INSERT INTO integration_outbox(event_id, routing_key, payload)
SELECT evidence.event_id, 'audit.gate-station-service',
    jsonb_build_object('eventId', evidence.event_id, 'eventType', 'AUDIT_RECORDED',
        'source', 'gate-station-service', 'actor', evidence.actor_id,
        'entityId', evidence.target_id::text, 'action', evidence.event_type,
        'factoryId', coalesce((SELECT factory_id FROM stations WHERE station_id = evidence.target_id), (SELECT factory_id FROM factories WHERE factory_id = evidence.target_id)), 'occurredAt', evidence.occurred_at, 'version', 1)
FROM station_audit_events evidence ON CONFLICT DO NOTHING;

CREATE FUNCTION protect_integration_outbox() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (to_jsonb(OLD) - 'published_at') IS DISTINCT FROM (to_jsonb(NEW) - 'published_at') THEN
        RAISE EXCEPTION 'Outbox evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER integration_outbox_immutable BEFORE UPDATE OR DELETE ON integration_outbox
    FOR EACH ROW EXECUTE FUNCTION protect_integration_outbox();
