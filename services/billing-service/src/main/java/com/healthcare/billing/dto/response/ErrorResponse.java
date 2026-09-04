package com.healthcare.billing.dto.response;

import java.time.Instant;
import java.util.List;

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
