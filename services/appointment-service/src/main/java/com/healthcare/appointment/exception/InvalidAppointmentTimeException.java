package com.healthcare.appointment.exception;

import org.springframework.http.HttpStatus;

public class InvalidAppointmentTimeException extends ApiException {
    public InvalidAppointmentTimeException(String reason) {
        super(HttpStatus.BAD_REQUEST, "INVALID_APPOINTMENT_TIME", reason);
    }
}
