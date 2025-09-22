package com.abada.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessInstanceDTO(
        String id,
        String currentActivityId,
        Map<String, Object> variables,
        boolean waitingForUserTask,
        boolean isCompleted,
        Instant startDate,
        Instant endDate
) {}
