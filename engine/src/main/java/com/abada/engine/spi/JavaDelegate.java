package com.abada.engine.spi;

/**
 * Interface for all service task delegate classes.
 * Implementations of this interface can be referenced by the `camunda:class`
 * attribute in a BPMN service task.
 */
public interface JavaDelegate {

    /**
     * The method called by the engine when a service task is executed.
     *
     * @param execution The execution context, providing access to process variables.
     * @throws Exception if an error occurs during execution.
     */
    void execute(DelegateExecution execution) throws Exception;
}
