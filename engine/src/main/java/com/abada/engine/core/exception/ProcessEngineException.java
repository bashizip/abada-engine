package com.abada.engine.core.exception;

/**
 * Custom runtime exception for business rule violations and other
 * known error states within the Abada process engine.
 */
public class ProcessEngineException extends RuntimeException {

    public ProcessEngineException(String message) {
        super(message);
    }

    public ProcessEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
