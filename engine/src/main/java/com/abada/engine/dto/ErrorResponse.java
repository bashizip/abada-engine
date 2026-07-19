package com.abada.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/** Stable error envelope for every public API failure. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "ApiError", description = "Machine-readable API error")
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        Map<String, Object> details) {

    public ErrorResponse(int status, String code, String message, String path, String traceId,
            Map<String, Object> details) {
        this(Instant.now(), status, code, message, path, traceId, details == null ? Map.of() : Map.copyOf(details));
    }
}
