package com.abada.engine.dto;

/**
 * Request body for suspending or activating a process instance.
 */
public record SuspensionRequest(
        boolean suspended) {
}
