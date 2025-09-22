package com.abada.engine.dto;

/**
 * A standardized JSON response for returning API errors.
 *
 * @param status  The HTTP status code.
 * @param message A clear, user-friendly error message.
 * @param path    The URL path where the error occurred.
 */
public record ErrorResponse(int status, String message, String path) {
}
