package com.healthcare.billing.exception;

import org.springframework.http.HttpStatus;

public class PaymentOverpaymentException extends ApiException {
    public PaymentOverpaymentException() {
        super(HttpStatus.CONFLICT, "PAYMENT_OVERPAYMENT",
              "Payment would exceed the invoice total amount.");
    }
}
