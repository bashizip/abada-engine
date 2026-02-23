package com.abada.engine.spi;

import java.util.Map;

/**
 * Provides access to the context of a running process instance for a JavaDelegate.
 */
public interface DelegateExecution {

    /**
     * Returns the unique ID of the current process instance.
     */
    String getProcessInstanceId();

    /**
     * Returns an unmodifiable map of all variables in the current scope.
     */
    Map<String, Object> getVariables();

    /**
     * Retrieves a single process variable by its name.
     *
     * @param name The name of the variable.
     * @return The value of the variable, or null if it doesn't exist.
     */
    Object getVariable(String name);

    /**
     * Sets a process variable.
     * If a variable with the same name already exists, it will be overwritten.
     *
     * @param name The name of the variable.
     * @param value The value of the variable.
     */
    void setVariable(String name, Object value);

}
