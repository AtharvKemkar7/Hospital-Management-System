package com.healthcare.prescription.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {
    public InvalidStatusTransitionException(String from, String to) {
        super(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
              "Cannot transition prescription from " + from + " to " + to + ".");
    }
}
