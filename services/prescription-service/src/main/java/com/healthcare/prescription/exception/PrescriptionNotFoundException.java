package com.healthcare.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionNotFoundException extends ApiException {
    public PrescriptionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PRESCRIPTION_NOT_FOUND", "Prescription not found.");
    }
}
