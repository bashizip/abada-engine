package com.abada.engine.api;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler to catch specific application exceptions and
 * transform them into standardized, user-friendly JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles known business rule violations from the process engine.
     *
     * @param ex      The caught ProcessEngineException.
     * @param request The current web request.
     * @return A ResponseEntity containing a structured error message with a 400 Bad Request status.
     */
    @ExceptionHandler(ProcessEngineException.class)
    public ResponseEntity<ErrorResponse> handleProcessEngineException(ProcessEngineException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
