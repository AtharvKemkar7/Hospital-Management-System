package com.healthcare.doctor.exception;

import org.springframework.http.HttpStatus;

public class DoctorAlreadyExistsException extends ApiException {
    public DoctorAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "DOCTOR_ALREADY_EXISTS",
              "A doctor profile already exists for this user or license number.");
    }
}
