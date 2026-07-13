ALTER TABLE process_definitions RENAME COLUMN id TO deployment_id;
ALTER TABLE process_definitions ADD COLUMN process_key VARCHAR(255);
ALTER TABLE process_definitions ADD COLUMN version INTEGER DEFAULT 1;
ALTER TABLE process_definitions ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE process_definitions ADD COLUMN checksum VARCHAR(64);
UPDATE process_definitions SET process_key = deployment_id WHERE process_key IS NULL;
ALTER TABLE process_definitions ALTER COLUMN process_key SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN version SET NOT NULL;
ALTER TABLE process_definitions ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE process_definitions ADD CONSTRAINT uk_process_definition_key_version UNIQUE (process_key, version);

ALTER TABLE process_instances ADD COLUMN process_definition_deployment_id VARCHAR(255);
ALTER TABLE process_instances ADD COLUMN active_tokens_json TEXT DEFAULT '[]';
ALTER TABLE process_instances ADD COLUMN join_expected_tokens_json TEXT DEFAULT '{}';
ALTER TABLE process_instances ADD COLUMN join_arrived_tokens_json TEXT DEFAULT '{}';
ALTER TABLE process_instances ADD COLUMN entity_version BIGINT DEFAULT 0;
UPDATE process_instances SET process_definition_deployment_id = process_definition_id
WHERE process_definition_deployment_id IS NULL;
ALTER TABLE process_instances ALTER COLUMN active_tokens_json SET NOT NULL;
ALTER TABLE process_instances ALTER COLUMN join_expected_tokens_json SET NOT NULL;
ALTER TABLE process_instances ALTER COLUMN join_arrived_tokens_json SET NOT NULL;
ALTER TABLE process_instances ALTER COLUMN entity_version SET NOT NULL;

ALTER TABLE tasks ADD COLUMN entity_version BIGINT DEFAULT 0;
ALTER TABLE tasks ALTER COLUMN entity_version SET NOT NULL;

ALTER TABLE jobs ADD COLUMN status VARCHAR(32) DEFAULT 'AVAILABLE';
ALTER TABLE jobs ADD COLUMN lease_owner VARCHAR(255);
ALTER TABLE jobs ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs ADD COLUMN attempts INTEGER DEFAULT 0;
ALTER TABLE jobs ADD COLUMN max_attempts INTEGER DEFAULT 3;
ALTER TABLE jobs ADD COLUMN last_error TEXT;
ALTER TABLE jobs ADD COLUMN entity_version BIGINT DEFAULT 0;
ALTER TABLE jobs ALTER COLUMN status SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN attempts SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN max_attempts SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN entity_version SET NOT NULL;

ALTER TABLE external_tasks ADD COLUMN entity_version BIGINT DEFAULT 0;
ALTER TABLE external_tasks ALTER COLUMN entity_version SET NOT NULL;

CREATE TABLE event_subscriptions (
    id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255) NOT NULL,
    activity_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    correlation_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    entity_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_event_subscription UNIQUE (process_instance_id, activity_id)
);
CREATE INDEX idx_event_subscription_message
    ON event_subscriptions(event_type, event_name, correlation_key, consumed_at);

CREATE TABLE activity_history (
    id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255),
    process_definition_id VARCHAR(255),
    activity_id VARCHAR(255),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    trace_id VARCHAR(64),
    details_json TEXT NOT NULL
);
CREATE INDEX idx_activity_history_instance_time
    ON activity_history(process_instance_id, occurred_at);

CREATE TABLE outbox_events (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(published_at, occurred_at);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    operation VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
