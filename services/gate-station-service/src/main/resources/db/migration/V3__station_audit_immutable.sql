CREATE FUNCTION protect_station_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (to_jsonb(OLD) - 'published_at') IS DISTINCT FROM (to_jsonb(NEW) - 'published_at') THEN
        RAISE EXCEPTION 'Station audit evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER station_audit_immutable BEFORE UPDATE OR DELETE ON station_audit_events
    FOR EACH ROW EXECUTE FUNCTION protect_station_audit();
