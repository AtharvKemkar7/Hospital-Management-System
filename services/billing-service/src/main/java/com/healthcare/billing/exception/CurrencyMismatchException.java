package com.healthcare.billing.exception;

import org.springframework.http.HttpStatus;

public class CurrencyMismatchException extends ApiException {
    public CurrencyMismatchException() {
        super(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH",
              "Payment currency does not match the invoice currency.");
    }
}
