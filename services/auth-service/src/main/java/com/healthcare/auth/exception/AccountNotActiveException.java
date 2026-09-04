package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApiException {
    public AccountNotActiveException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.");
    }
}
