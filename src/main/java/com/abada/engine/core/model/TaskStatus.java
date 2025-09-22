package com.abada.engine.core.model;

/**
 * Represents the explicit lifecycle status of a user task.
 */
public enum  TaskStatus {
    /**
     * The task has been created and is waiting in a pool to be claimed by a user.
     * UI equivalent: Pending
     */
    AVAILABLE,

    /**
     * The task has been assigned to a specific user who is actively working on it.
     * UI equivalent: In Progress
     */
    CLAIMED,

    /**
     * The task has been successfully completed, and the process instance has advanced.
     * UI equivalent: Done
     */
    COMPLETED,

    /**
     * The task has been temporarily paused and is not currently being worked on.
     * UI equivalent: On Hold
     */
    SUSPENDED,

    /**
     * The task has been explicitly terminated and will not be completed.
     * UI equivalent: Cancelled
     */
    CANCELLED,

    /**
     * The task has been reassigned to another user or team.
     * UI equivalent: Delegated
     */
    DELEGATED,

    /**
     * The task has breached a deadline or SLA and has been raised to another actor or level.
     * UI equivalent: Escalated
     */
    ESCALATED,

    /**
     * The task was not completed before its deadline.
     * UI equivalent: Timed Out
     */
    EXPIRED,

    /**
     * The task was attempted but ended in an error state.
     * UI equivalent: Failed
     */
    FAILED
}
