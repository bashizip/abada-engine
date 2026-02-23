package com.abada.engine.dto;

/**
 * DTO for failed job information returned to Orun.
 */
public record FailedJobDTO(
        String jobId,
        String processInstanceId,
        String activityId,
        String exceptionMessage,
        Integer retries) {
}
