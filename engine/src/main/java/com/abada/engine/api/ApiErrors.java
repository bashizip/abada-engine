package com.abada.engine.api;

import com.abada.engine.dto.ErrorResponse;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class ApiErrors {
    private ApiErrors() {}

    public static ErrorResponse response(HttpStatus status, ApiErrorCode code, String message,
            HttpServletRequest request) {
        return response(status, code, message, request.getRequestURI(), Map.of());
    }

    public static ErrorResponse response(HttpStatus status, ApiErrorCode code, String message,
            String path, Map<String, Object> details) {
        var spanContext = Span.current().getSpanContext();
        String traceId = spanContext.isValid() ? spanContext.getTraceId() : null;
        return new ErrorResponse(status.value(), code.name(), message, path, traceId, details);
    }
}
