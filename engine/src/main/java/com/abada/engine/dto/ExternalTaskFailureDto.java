package com.abada.engine.dto;

public record ExternalTaskFailureDto(
        String workerId,
        String errorMessage,
        String errorDetails,
        Integer retries,
        Long retryTimeout) {
}
