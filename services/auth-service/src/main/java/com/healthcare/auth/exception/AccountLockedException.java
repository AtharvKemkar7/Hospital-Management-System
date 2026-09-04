package com.healthcare.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {
    public AccountLockedException() {
        // Same wording as AuthenticationFailedException on purpose: we do not
        // reveal that the account exists, only that the credentials are
        // unusable right now.
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.");
    }
}
