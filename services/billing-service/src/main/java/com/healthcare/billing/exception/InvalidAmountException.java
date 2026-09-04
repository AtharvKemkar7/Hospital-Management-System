package com.healthcare.billing.exception;

import org.springframework.http.HttpStatus;

public class InvalidAmountException extends ApiException {
    public InvalidAmountException(String reason) {
        super(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", reason);
    }
}
