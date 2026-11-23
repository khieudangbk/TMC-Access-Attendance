ALTER TABLE qr_audit_events ADD COLUMN factory_id UUID;
UPDATE qr_audit_events audit SET factory_id = credential.factory_id
FROM qr_credentials credential WHERE credential.qr_id = audit.qr_id;

CREATE OR REPLACE FUNCTION enqueue_audit_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO integration_outbox(event_id, routing_key, payload) VALUES (NEW.event_id, 'audit.qr-service',
        jsonb_build_object('eventId', NEW.event_id, 'eventType', 'AUDIT_RECORDED', 'source', 'qr-service',
            'actor', NEW.employee_id::text, 'entityId', coalesce(NEW.qr_id::text, NEW.employee_id::text),
            'action', NEW.event_type, 'factoryId', NEW.factory_id, 'occurredAt', NEW.occurred_at, 'version', 1));
    RETURN NEW;
END; $$;

CREATE FUNCTION protect_qr_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (to_jsonb(OLD) - 'published_at') IS DISTINCT FROM (to_jsonb(NEW) - 'published_at') THEN
        RAISE EXCEPTION 'QR audit evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER qr_audit_immutable BEFORE UPDATE OR DELETE ON qr_audit_events
    FOR EACH ROW EXECUTE FUNCTION protect_qr_audit();
