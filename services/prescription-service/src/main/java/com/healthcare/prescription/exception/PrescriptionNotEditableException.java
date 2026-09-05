package com.healthcare.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionNotEditableException extends ApiException {
    public PrescriptionNotEditableException() {
        super(HttpStatus.CONFLICT, "PRESCRIPTION_NOT_EDITABLE",
              "Prescription items can only be added while the prescription is in ISSUED state.");
    }
}
