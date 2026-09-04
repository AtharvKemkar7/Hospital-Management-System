package com.healthcare.billing.service;

import com.healthcare.billing.dto.request.CreateInvoiceRequest;
import com.healthcare.billing.dto.request.CreatePaymentRequest;
import com.healthcare.billing.entity.Invoice;
import com.healthcare.billing.entity.InvoiceItem;
import com.healthcare.billing.entity.InvoiceStatus;
import com.healthcare.billing.entity.Payment;
import com.healthcare.billing.exception.AccessDeniedException;
import com.healthcare.billing.exception.CurrencyMismatchException;
import com.healthcare.billing.exception.InvoiceNotFoundException;
import com.healthcare.billing.exception.InvalidStatusTransitionException;
import com.healthcare.billing.exception.PaymentOverpaymentException;
import com.healthcare.billing.repository.InvoiceItemRepository;
import com.healthcare.billing.repository.InvoiceRepository;
import com.healthcare.billing.repository.PaymentRepository;
import com.healthcare.billing.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic, authorization, and lifecycle management for the
 * Billing Service.
 *
 * <p><b>Authorization model</b> (per {@code docs/service-boundaries.md}
 * §8):
 * <ul>
 *   <li>{@code BILLING_STAFF}, {@code ADMIN} — full access to
 *       invoices, items, and payments.</li>
 *   <li>{@code PATIENT} — read access to <b>own</b> invoices and
 *       their payments (matched on {@code patientId} from the JWT;
 *       never on body-supplied identity).</li>
 *   <li>{@code DOCTOR}, {@code RECEPTIONIST} — denied. Default-deny.</li>
 * </ul>
 *
 * <p><b>Monetary policy</b> — see {@link Money}. All calculations
 * are server-side.
 *
 * <p><b>Concurrency</b> — invoices are versioned with JPA
 * {@code @Version} (optimistic locking). Payment recording reloads
 * the invoice within the same transaction and checks the running
 * payment total against the invoice total before committing.
 *
 * <p><b>Cross-service existence validation</b> — patientId and
 * appointmentId are trusted from the body. No REST client is
 * created. Documented as deferred.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final InvoiceItemRepository items;
    private final PaymentRepository payments;

    public InvoiceService(InvoiceRepository invoices,
                          InvoiceItemRepository items,
                          PaymentRepository payments) {
        this.invoices = invoices;
        this.items = items;
        this.payments = payments;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public Invoice create(Role currentRole, CreateInvoiceRequest req) {
        if (currentRole != Role.BILLING_STAFF && currentRole != Role.ADMIN) {
            throw new AccessDeniedException();
        }

        String currency = (req.currency() == null || req.currency().isBlank())
                ? "USD" : req.currency();

        // 1) Compute line totals and the invoice total SERVER-SIDE.
        //    Any client-supplied totals are ignored.
        List<BigDecimal> lineTotals = new ArrayList<>(req.items().size());
        List<InvoiceItem> toPersist = new ArrayList<>(req.items().size());

        for (var itemReq : req.items()) {
            BigDecimal lineTotal = Money.lineTotal(itemReq.unitPrice(), itemReq.quantity());
            lineTotals.add(lineTotal);
            toPersist.add(InvoiceItem.create(
                    /* invoiceId */ null,  // assigned after invoice persists
                    itemReq.source(),
                    itemReq.sourceId(),
                    itemReq.description(),
                    itemReq.quantity(),
                    itemReq.unitPrice(),
                    lineTotal));
        }
        BigDecimal total = Money.sum(lineTotals);

        Invoice invoice = Invoice.create(
                req.patientId(),
                req.appointmentId(),
                total,
                currency,
                req.dueAt());
        invoice = invoices.save(invoice);

        // Re-bind items to the now-known invoice id and persist them.
        for (InvoiceItem item : toPersist) {
            item.bindInvoiceId(invoice.getId());
            items.save(item);
        }
        return invoice;
    }

    // ------------------------------------------------------------------ issue

    @Transactional
    public Invoice issue(Role currentRole, UUID invoiceId) {
        if (currentRole != Role.BILLING_STAFF) {
            throw new AccessDeniedException();
        }
        Invoice i = loadOrThrow(invoiceId);
        if (i.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidStatusTransitionException(i.getStatus().name(), "ISSUED");
        }
        i.issue();
        return invoices.save(i);
    }

    @Transactional
    public Invoice voidInvoice(Role currentRole, UUID invoiceId) {
        if (currentRole != Role.BILLING_STAFF && currentRole != Role.ADMIN) {
            throw new AccessDeniedException();
        }
        Invoice i = loadOrThrow(invoiceId);
        if (i.getStatus() == InvoiceStatus.PAID
                || i.getStatus() == InvoiceStatus.REFUNDED
                || i.getStatus() == InvoiceStatus.VOID) {
            throw new InvalidStatusTransitionException(i.getStatus().name(), "VOID");
        }
        i.voidInvoice();
        return invoices.save(i);
    }

    // ------------------------------------------------------------------ payment

    @Transactional
    public Payment recordPayment(Role currentRole, UUID invoiceId, CreatePaymentRequest req) {
        if (currentRole != Role.BILLING_STAFF) {
            throw new AccessDeniedException();
        }
        Invoice i = loadOrThrow(invoiceId);
        if (i.getStatus() == InvoiceStatus.VOID || i.getStatus() == InvoiceStatus.REFUNDED) {
            throw new InvalidStatusTransitionException(i.getStatus().name(), "PAID");
        }
        String currency = (req.currency() == null || req.currency().isBlank())
                ? i.getCurrency() : req.currency();
        if (!currency.equals(i.getCurrency())) {
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = Money.requirePositive(req.amount(), "amount");

        // Running payment total must not exceed the invoice total.
        BigDecimal alreadyPaid = payments.findByInvoiceIdOrderByPaidAtDesc(i.getId()).stream()
                .map(Payment::getAmount)
                .reduce(Money.zero(), (a, b) -> Money.sum(List.of(a, b)));
        BigDecimal newTotal = Money.sum(List.of(alreadyPaid, amount));
        if (newTotal.compareTo(i.getTotalAmount()) > 0) {
            throw new PaymentOverpaymentException();
        }

        Payment p = Payment.create(
                i.getId(), req.method(), amount, currency,
                req.paidAt(), req.reference());
        p = payments.save(p);

        // Mark as PAID if fully covered. PARTIALLY_PAID is not in the
        // documented status set; we record the full payment only when
        // the cumulative amount exactly equals the invoice total.
        if (newTotal.compareTo(i.getTotalAmount()) == 0) {
            i.markPaid();
            invoices.save(i);
        }
        return p;
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public Invoice getAuthorized(UUID currentUserId, Role currentRole, UUID invoiceId) {
        Invoice i = loadOrThrow(invoiceId);
        authorizeRead(i, currentUserId, currentRole);
        return i;
    }

    @Transactional(readOnly = true)
    public List<InvoiceItem> listItems(UUID currentUserId, Role currentRole, UUID invoiceId) {
        getAuthorized(currentUserId, currentRole, invoiceId);
        return items.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<Payment> listPayments(UUID currentUserId, Role currentRole, UUID invoiceId) {
        getAuthorized(currentUserId, currentRole, invoiceId);
        return payments.findByInvoiceIdOrderByPaidAtDesc(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listMineAsPatient(UUID currentUserId) {
        return invoices.findByPatientIdOrderByCreatedAtDesc(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listAllForStaff() {
        return invoices.findAll();
    }

    // ------------------------------------------------------------------ helpers

    private Invoice loadOrThrow(UUID id) {
        return invoices.findById(id).orElseThrow(InvoiceNotFoundException::new);
    }

    /**
     * Object-level authorization. The database record is the source
     * of truth: we compare {@code invoice.patientId} with the JWT's
     * {@code userId}.
     */
    private void authorizeRead(Invoice i, UUID currentUserId, Role currentRole) {
        switch (currentRole) {
            case BILLING_STAFF, ADMIN -> { /* full read */ }
            case PATIENT -> {
                if (!i.getPatientId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            default -> throw new AccessDeniedException();
        }
    }

    /** True if the role is permitted to create or modify invoices. */
    public static boolean canManageInvoices(Role role) {
        return role == Role.BILLING_STAFF || role == Role.ADMIN;
    }
}
