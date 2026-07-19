ALTER TABLE external_tasks ADD COLUMN bpmn_error_code VARCHAR(255);
ALTER TABLE external_tasks ADD COLUMN bpmn_error_message TEXT;
ALTER TABLE external_tasks ADD COLUMN trace_parent VARCHAR(128);

CREATE INDEX idx_external_tasks_worker_lock
    ON external_tasks(worker_id, status, lock_expiration_time);
