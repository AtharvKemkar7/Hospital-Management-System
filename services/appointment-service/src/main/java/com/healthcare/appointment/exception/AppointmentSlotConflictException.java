package com.healthcare.appointment.exception;

import org.springframework.http.HttpStatus;

public class AppointmentSlotConflictException extends ApiException {
    public AppointmentSlotConflictException() {
        super(HttpStatus.CONFLICT, "APPOINTMENT_SLOT_CONFLICT",
              "Another active appointment already exists for this doctor at the requested time.");
    }
}
