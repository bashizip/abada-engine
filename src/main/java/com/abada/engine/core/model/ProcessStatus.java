package com.abada.engine.core.model;

/**
 * Represents the high-level status of a process instance.
 */
public enum ProcessStatus {
    /**
     * The process instance is currently active and has running tokens.
     */
    RUNNING,

    /**
     * The process instance has reached an end event and has no more running tokens.
     */
    COMPLETED,

    /**
     * The process instance was terminated due to an unrecoverable error.
     */
    FAILED
}
