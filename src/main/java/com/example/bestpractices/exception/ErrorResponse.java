package com.example.bestpractices.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Best practices demonstrated:
 * - Consistent error envelope across all error types
 * - @JsonInclude(NON_NULL) suppresses the "errors" field when there are no field violations
 * - Instant timestamp lets clients correlate errors to logs via the correlation ID
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    Instant timestamp;
    int status;
    String error;
    String message;
    String path;
    String correlationId;
    List<FieldError> errors;

    @Value
    @Builder
    public static class FieldError {
        String field;
        String message;
    }
}
