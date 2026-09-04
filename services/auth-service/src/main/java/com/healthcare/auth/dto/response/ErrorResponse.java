package com.healthcare.auth.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope. Format is locked by {@code docs/api-design.md}.
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
