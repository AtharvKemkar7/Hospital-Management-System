package com.healthcare.doctor.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends ApiException {
    public AccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN",
              "You do not have permission to perform this action.");
    }
}
