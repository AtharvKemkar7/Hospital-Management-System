package com.healthcare.doctor.exception;

import org.springframework.http.HttpStatus;

public class DoctorNotFoundException extends ApiException {
    public DoctorNotFoundException() {
        super(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor profile not found.");
    }
}
