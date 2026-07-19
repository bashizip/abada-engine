package com.abada.engine.api;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final ApiErrorCode code;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, ApiErrorCode code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, ApiErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus status() { return status; }
    public ApiErrorCode code() { return code; }
    public Map<String, Object> details() { return details; }
}
