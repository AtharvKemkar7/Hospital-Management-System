package com.healthcare.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test void acceptsStrongPassword() {
        assertThat(policy.validate("Sup3rSafe!Pass")).isNull();
    }

    @Test void rejectsTooShort() {
        assertThat(policy.validate("Aa1short")).isNotNull();
    }

    @Test void rejectsNoDigit() {
        assertThat(policy.validate("NoDigitsHere!")).isNotNull();
    }

    @Test void rejectsNoLetter() {
        assertThat(policy.validate("123456789012")).isNotNull();
    }

    @Test void rejectsBlank() {
        assertThat(policy.validate("            ")).isNotNull();
    }

    @Test void rejectsNull() {
        assertThat(policy.validate(null)).isNotNull();
    }

    @Test void rejectsTooLong() {
        String longPw = "Aa1" + "x".repeat(200);
        assertThat(policy.validate(longPw)).isNotNull();
    }
}
