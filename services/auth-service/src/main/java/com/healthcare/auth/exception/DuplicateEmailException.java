package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {
    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, "CONFLICT", "An account with this email already exists.");
    }
}
