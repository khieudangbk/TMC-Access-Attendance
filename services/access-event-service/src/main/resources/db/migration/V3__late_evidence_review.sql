CREATE TABLE access_evidence_reviews (
    decision_id UUID PRIMARY KEY,
    confirmation_id UUID NOT NULL UNIQUE REFERENCES command_confirmations(confirmation_id),
    actor_id UUID NOT NULL,
    approved BOOLEAN NOT NULL,
    reason VARCHAR(500) NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    event_id UUID REFERENCES access_events(event_id),
    effect VARCHAR(40) NOT NULL
);
CREATE TABLE integration_outbox (
    event_id UUID PRIMARY KEY,
    routing_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
CREATE INDEX integration_outbox_pending ON integration_outbox(created_at) WHERE published_at IS NULL;
CREATE TRIGGER confirmation_immutable BEFORE UPDATE OR DELETE ON command_confirmations
    FOR EACH ROW EXECUTE FUNCTION reject_access_event_mutation();
CREATE TRIGGER review_immutable BEFORE UPDATE OR DELETE ON access_evidence_reviews
    FOR EACH ROW EXECUTE FUNCTION reject_access_event_mutation();
CREATE FUNCTION protect_review_outbox() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' OR (to_jsonb(OLD) - 'published_at') IS DISTINCT FROM (to_jsonb(NEW) - 'published_at') THEN
        RAISE EXCEPTION 'Review outbox evidence is immutable';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER review_outbox_immutable BEFORE UPDATE OR DELETE ON integration_outbox
    FOR EACH ROW EXECUTE FUNCTION protect_review_outbox();
