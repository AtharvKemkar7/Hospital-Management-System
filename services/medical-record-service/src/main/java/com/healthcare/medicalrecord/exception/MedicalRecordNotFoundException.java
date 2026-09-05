package com.healthcare.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordNotFoundException extends ApiException {
    public MedicalRecordNotFoundException() {
        super(HttpStatus.NOT_FOUND, "MEDICAL_RECORD_NOT_FOUND", "Medical record not found.");
    }
}
