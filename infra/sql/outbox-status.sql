-- Run on each owning database using an operator account. Never aggregate by cross-database joins.
-- Identity, QR, Gate, Attendance, HR:
SELECT count(*) AS pending_events, min(created_at) AS oldest_pending_at
FROM integration_outbox WHERE published_at IS NULL;

-- Access Event database only:
SELECT count(*) AS pending_events, min(created_at) AS oldest_pending_at
FROM outbox_events WHERE published_at IS NULL;
