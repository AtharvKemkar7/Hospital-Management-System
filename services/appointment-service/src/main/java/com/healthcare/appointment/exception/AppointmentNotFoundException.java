package com.healthcare.appointment.exception;

import org.springframework.http.HttpStatus;

public class AppointmentNotFoundException extends ApiException {
    public AppointmentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found.");
    }
}
