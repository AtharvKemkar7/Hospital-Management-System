package com.healthcare.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthcare.billing.entity.Invoice;
import com.healthcare.billing.entity.InvoiceItem;
import com.healthcare.billing.entity.InvoiceItemSource;
import com.healthcare.billing.entity.InvoiceStatus;
import com.healthcare.billing.entity.Payment;
import com.healthcare.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Safe projection of an invoice. Items and (optionally) payments are
 * embedded; identity fields are minimal — no email, no full name, no
 * doctorId.
 */
public record InvoiceResponse(
        UUID id,
        UUID patientId,
        UUID appointmentId,
        InvoiceStatus status,
        BigDecimal totalAmount,
        String currency,
        Instant issuedAt,
        Instant dueAt,
        Instant createdAt,
        Instant updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<InvoiceItemResponse> items,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<PaymentResponse> payments
) {

    public static InvoiceResponse from(Invoice i, List<InvoiceItem> items, List<Payment> payments) {
        return new InvoiceResponse(
                i.getId(),
                i.getPatientId(),
                i.getAppointmentId(),
                i.getStatus(),
                i.getTotalAmount(),
                i.getCurrency(),
                i.getIssuedAt(),
                i.getDueAt(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                items == null ? null : items.stream().map(InvoiceItemResponse::from).toList(),
                payments == null ? null : payments.stream().map(PaymentResponse::from).toList()
        );
    }

    public static InvoiceResponse summaryOnly(Invoice i) {
        return new InvoiceResponse(
                i.getId(), i.getPatientId(), i.getAppointmentId(),
                i.getStatus(), i.getTotalAmount(), i.getCurrency(),
                i.getIssuedAt(), i.getDueAt(),
                i.getCreatedAt(), i.getUpdatedAt(),
                null, null
        );
    }

    public record InvoiceItemResponse(
            UUID id,
            InvoiceItemSource source,
            UUID sourceId,
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static InvoiceItemResponse from(InvoiceItem i) {
            return new InvoiceItemResponse(
                    i.getId(), i.getSource(), i.getSourceId(), i.getDescription(),
                    i.getQuantity(), i.getUnitPrice(), i.getLineTotal(),
                    i.getCreatedAt(), i.getUpdatedAt()
            );
        }
    }

    public record PaymentResponse(
            UUID id,
            PaymentMethod method,
            BigDecimal amount,
            String currency,
            Instant paidAt,
            String reference,
            Instant createdAt
    ) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(
                    p.getId(), p.getMethod(), p.getAmount(), p.getCurrency(),
                    p.getPaidAt(), p.getReference(), p.getCreatedAt()
            );
        }
    }
}
