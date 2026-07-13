CREATE TABLE process_definitions (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    documentation TEXT,
    bpmn_xml TEXT,
    candidate_starter_groups VARCHAR(255),
    candidate_starter_users VARCHAR(255)
);
CREATE TABLE process_instances (
    id VARCHAR(255) PRIMARY KEY,
    process_definition_id VARCHAR(255),
    current_activity_id VARCHAR(255),
    status VARCHAR(32),
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE,
    started_by VARCHAR(255) NOT NULL DEFAULT 'system',
    variables_json TEXT NOT NULL,
    suspended BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tasks (
    id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255) NOT NULL,
    assignee VARCHAR(255),
    task_definition_key VARCHAR(255),
    name VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE
);

CREATE TABLE task_candidate_users (
    task_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255)
);

CREATE TABLE task_candidate_groups (
    task_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255)
);

CREATE TABLE jobs (
    id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255),
    event_id VARCHAR(255),
    execution_timestamp TIMESTAMP WITH TIME ZONE
);

CREATE TABLE external_tasks (
    id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255),
    topic_name VARCHAR(255),
    status VARCHAR(32),
    worker_id VARCHAR(255),
    lock_expiration_time TIMESTAMP WITH TIME ZONE,
    exception_message TEXT,
    exception_stacktrace TEXT,
    retries INTEGER DEFAULT 3,
    activity_id VARCHAR(255)
);
