package com.healthcare.appointment.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {
    public InvalidStatusTransitionException(String from, String to) {
        super(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
              "Cannot transition appointment from " + from + " to " + to + ".");
    }
}
