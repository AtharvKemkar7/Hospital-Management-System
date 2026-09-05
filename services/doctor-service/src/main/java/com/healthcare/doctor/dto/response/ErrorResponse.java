package com.healthcare.doctor.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope. Mirrors the Auth Service / Patient Service
 * error contract defined in {@code docs/api-design.md} §5.
 */
public record ErrorResponse(
        Instant timestamp,
        int     status,
        String  code,
        String  message,
        String  path,
        String  correlationId,
        List<ErrorDetail> details
) {

    public static ErrorResponse of(int status, String code, String message,
                                   String path, String correlationId,
                                   List<ErrorDetail> details) {
        return new ErrorResponse(Instant.now(), status, code, message,
                                 path, correlationId, details);
    }
}
