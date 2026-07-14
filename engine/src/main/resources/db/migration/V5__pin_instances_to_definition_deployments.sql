ALTER TABLE process_instances
    ALTER COLUMN process_definition_deployment_id SET NOT NULL;

ALTER TABLE process_instances
    ADD CONSTRAINT fk_process_instance_definition_deployment
    FOREIGN KEY (process_definition_deployment_id)
    REFERENCES process_definitions(deployment_id);
