package com.bookify.backend.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        Map<String, String> validationErrors
) {
    public ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path,
            String correlationId
    ) {
        this(timestamp, status, error, code, message, path, correlationId, null);
    }
}
