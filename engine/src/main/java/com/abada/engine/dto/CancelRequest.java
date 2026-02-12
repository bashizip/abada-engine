package com.abada.engine.dto;

/**
 * Request body for cancelling a process instance.
 */
public record CancelRequest(
        String reason) {
}
