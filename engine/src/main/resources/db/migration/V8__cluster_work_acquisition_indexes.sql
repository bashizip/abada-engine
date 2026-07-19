CREATE INDEX idx_jobs_available_acquisition
    ON jobs(status, execution_timestamp, id);

CREATE INDEX idx_jobs_expired_lease_acquisition
    ON jobs(status, lease_expires_at, id);

CREATE INDEX idx_external_tasks_acquisition
    ON external_tasks(topic_name, status, lock_expiration_time, id);
