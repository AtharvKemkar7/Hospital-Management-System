package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {
    public InvalidTokenException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static InvalidTokenException invalid() {
        return new InvalidTokenException("UNAUTHORIZED", "Invalid token.");
    }

    public static InvalidTokenException revoked() {
        return new InvalidTokenException("TOKEN_REVOKED", "Token has been revoked.");
    }

    public static InvalidTokenException expired() {
        return new InvalidTokenException("TOKEN_EXPIRED", "Token has expired.");
    }
}
