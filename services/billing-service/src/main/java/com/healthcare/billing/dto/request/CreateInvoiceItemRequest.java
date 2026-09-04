package com.healthcare.billing.dto.request;

import com.healthcare.billing.entity.InvoiceItemSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line item for {@code CreateInvoiceRequest}.
 *
 * <p>The server computes {@code lineTotal} as
 * {@code quantity * unitPrice} (rounded HALF_UP, scale 4). Any
 * client-supplied line total is ignored.
 */
public record CreateInvoiceItemRequest(

        @NotNull
        InvoiceItemSource source,

        UUID sourceId,

        @NotBlank
        @Size(max = 500)
        String description,

        @Min(1) @Max(10000)
        int quantity,

        @NotNull
        @PositiveOrZero
        BigDecimal unitPrice
) { }
