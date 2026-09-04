package com.healthcare.billing.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {
    public InvalidStatusTransitionException(String from, String to) {
        super(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
              "Cannot transition invoice from " + from + " to " + to + ".");
    }
}
