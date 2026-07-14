CREATE INDEX idx_tasks_process_instance_status
    ON tasks(process_instance_id, status);

CREATE INDEX idx_tasks_assignee_status
    ON tasks(assignee, status);

CREATE INDEX idx_tasks_definition_instance
    ON tasks(task_definition_key, process_instance_id, start_date);

CREATE INDEX idx_task_candidate_users_user_task
    ON task_candidate_users(user_id, task_id);

CREATE INDEX idx_task_candidate_groups_group_task
    ON task_candidate_groups(group_id, task_id);
