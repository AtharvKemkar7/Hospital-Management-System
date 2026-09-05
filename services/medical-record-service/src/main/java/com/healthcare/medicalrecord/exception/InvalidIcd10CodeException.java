package com.healthcare.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class InvalidIcd10CodeException extends ApiException {
    public InvalidIcd10CodeException(String code) {
        super(HttpStatus.BAD_REQUEST, "INVALID_ICD10_CODE",
              "icd10Code '" + code + "' is not a valid ICD-10 code.");
    }
}
