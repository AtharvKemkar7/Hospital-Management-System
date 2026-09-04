package com.healthcare.billing.dto.request;

import com.healthcare.billing.entity.InvoiceItemSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/invoices}.
 *
 * <p>Client-supplied totals are <b>not</b> accepted: the server
 * computes {@code lineTotal} for each item and {@code totalAmount}
 * for the invoice from {@code quantity * unitPrice}. Currency is
 * accepted as an ISO-4217 alpha-3 code; the server may also enforce
 * a default.
 *
 * <p>Cross-service existence validation (does this patient exist?
 * does this appointment exist?) is <b>deferred</b>. The Billing
 * Service trusts the supplied IDs and never queries another
 * service's database.
 */
public record CreateInvoiceRequest(

        @NotNull
        UUID patientId,

        UUID appointmentId,

        @Pattern(regexp = "^[A-Z]{3}$",
                 message = "currency must be a 3-letter ISO-4217 code")
        String currency,

        Instant dueAt,

        @NotNull
        @NotEmpty
        @Valid
        List<CreateInvoiceItemRequest> items
) { }
