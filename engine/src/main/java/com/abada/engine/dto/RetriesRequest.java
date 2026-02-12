package com.abada.engine.dto;

/**
 * Request body for setting job retries.
 */
public record RetriesRequest(
        Integer retries) {
}
