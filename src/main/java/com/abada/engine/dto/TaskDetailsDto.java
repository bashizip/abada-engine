package com.abada.engine.dto;

import com.abada.engine.core.model.TaskInstance;

import java.util.List;
import java.util.Map;

/**
 * Represents the detailed view of a single user task, including its associated process variables.
 * This is the payload returned by the `GET /api/v1/tasks/{id}` endpoint.
 */
public record TaskDetailsDto(
        String id,
        String taskDefinitionKey,
        String name,
        String assignee,
        List<String> candidateUsers,
        List<String> candidateGroups,
        String processInstanceId,
        Map<String, Object> variables
) {
    public static TaskDetailsDto from(TaskInstance task, Map<String, Object> variables) {
        return new TaskDetailsDto(
                task.getId(),
                task.getTaskDefinitionKey(),
                task.getName(),
                task.getAssignee(),
                task.getCandidateUsers(),
                task.getCandidateGroups(),
                task.getProcessInstanceId(),
                variables
        );
    }
}
