package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when credentials cannot be authenticated. The message is intentionally
 * generic so the caller cannot enumerate accounts.
 */
public class AuthenticationFailedException extends ApiException {
    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.");
    }
}
