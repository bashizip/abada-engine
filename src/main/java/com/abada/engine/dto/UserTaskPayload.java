package com.abada.engine.dto;

import java.util.List;

public record UserTaskPayload(
        String taskDefinitionKey,
        String name,
        String assignee,
        List<String> candidateUsers,
        List<String> candidateGroups
) {}
