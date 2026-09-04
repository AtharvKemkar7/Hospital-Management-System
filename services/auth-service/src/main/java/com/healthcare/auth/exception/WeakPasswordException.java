package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

public class WeakPasswordException extends ApiException {

    private final String reason;

    public WeakPasswordException(String reason) {
        super(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Password does not meet the security policy.");
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
