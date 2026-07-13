CREATE INDEX idx_process_instances_status_definition
    ON process_instances(status, process_definition_id);
