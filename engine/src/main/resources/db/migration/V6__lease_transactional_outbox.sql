ALTER TABLE outbox_events ADD COLUMN lease_owner VARCHAR(255);
ALTER TABLE outbox_events ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_events ADD COLUMN entity_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_outbox_dispatchable
    ON outbox_events(published_at, next_attempt_at, lease_expires_at, occurred_at);
