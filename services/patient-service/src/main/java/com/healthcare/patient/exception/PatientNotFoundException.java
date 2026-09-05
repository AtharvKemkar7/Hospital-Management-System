package com.healthcare.patient.exception;

import org.springframework.http.HttpStatus;

public class PatientNotFoundException extends ApiException {
    public PatientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Patient profile not found.");
    }
}
