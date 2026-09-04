package com.healthcare.billing.dto.request;

import com.healthcare.billing.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Body of {@code POST /api/v1/invoices/{id}/payments}.
 *
 * <p>No real payment provider is integrated in Phase 7; the request
 * is recorded as an internal payment record and the invoice status
 * is updated server-side. The {@code reference} field is intended
 * for future payment-provider transaction ids; it is stored as a
 * free-form short string.
 */
public record CreatePaymentRequest(

        @NotNull
        PaymentMethod method,

        @NotNull
        @Positive
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$",
                 message = "currency must be a 3-letter ISO-4217 code")
        String currency,

        Instant paidAt,

        @Size(max = 200)
        String reference
) { }
