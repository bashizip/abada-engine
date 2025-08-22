package com.abada.engine.dto;

import java.util.List;

public record UserTaskPayloadDTO(
        String taskDefinitionKey,
        String name,
        String assignee,
        List<String> candidateUsers,
        List<String> candidateGroups
) {}
