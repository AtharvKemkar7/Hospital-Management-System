package com.healthcare.patient.exception;

import org.springframework.http.HttpStatus;

public class PatientAlreadyExistsException extends ApiException {
    public PatientAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "PATIENT_ALREADY_EXISTS",
              "A patient profile already exists for this user.");
    }
}
