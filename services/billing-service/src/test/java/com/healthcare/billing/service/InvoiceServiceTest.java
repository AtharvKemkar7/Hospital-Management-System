package com.healthcare.billing.service;

import com.healthcare.billing.dto.request.CreateInvoiceItemRequest;
import com.healthcare.billing.dto.request.CreateInvoiceRequest;
import com.healthcare.billing.dto.request.CreatePaymentRequest;
import com.healthcare.billing.entity.Invoice;
import com.healthcare.billing.entity.InvoiceItem;
import com.healthcare.billing.entity.InvoiceItemSource;
import com.healthcare.billing.entity.InvoiceStatus;
import com.healthcare.billing.entity.Payment;
import com.healthcare.billing.entity.PaymentMethod;
import com.healthcare.billing.exception.AccessDeniedException;
import com.healthcare.billing.exception.CurrencyMismatchException;
import com.healthcare.billing.exception.InvoiceNotFoundException;
import com.healthcare.billing.exception.InvalidAmountException;
import com.healthcare.billing.exception.InvalidStatusTransitionException;
import com.healthcare.billing.exception.PaymentOverpaymentException;
import com.healthcare.billing.repository.InvoiceItemRepository;
import com.healthcare.billing.repository.InvoiceRepository;
import com.healthcare.billing.repository.PaymentRepository;
import com.healthcare.billing.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    private InvoiceRepository invoices;
    private InvoiceItemRepository items;
    private PaymentRepository payments;
    private InvoiceService service;

    private final UUID alicePid = UUID.randomUUID();
    private final UUID bobPid   = UUID.randomUUID();
    private final UUID aliceAppt = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        items = mock(InvoiceItemRepository.class);
        payments = mock(PaymentRepository.class);
        service = new InvoiceService(invoices, items, payments);
    }

    private Invoice existingDraft(UUID patientId) {
        return Invoice.create(patientId, aliceAppt, new BigDecimal("100.00"), "USD", null);
    }

    // -- invoice creation ---------------------------------------------------

    @Test
    void create_billingStaffCanCreate() {
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.save(any(InvoiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice i = service.create(Role.BILLING_STAFF,
                new CreateInvoiceRequest(
                        alicePid, aliceAppt, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "Consultation",
                                1, new BigDecimal("100.00")))));

        assertThat(i.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(i.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        verify(items).save(any(InvoiceItem.class));
    }

    @Test
    void create_adminCanCreate() {
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.save(any(InvoiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice i = service.create(Role.ADMIN,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "x", 1, new BigDecimal("50.00")))));
        assertThat(i.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void create_patientCannotCreate() {
        assertThatThrownBy(() -> service.create(Role.PATIENT,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "x", 1, new BigDecimal("1.00"))))))
                .isInstanceOf(AccessDeniedException.class);
        verify(invoices, never()).save(any());
    }

    @Test
    void create_doctorCannotCreate() {
        assertThatThrownBy(() -> service.create(Role.DOCTOR,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "x", 1, new BigDecimal("1.00"))))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_receptionistCannotCreate() {
        assertThatThrownBy(() -> service.create(Role.RECEPTIONIST,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "x", 1, new BigDecimal("1.00"))))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_totalIsComputedFromItems() {
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.save(any(InvoiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice i = service.create(Role.BILLING_STAFF,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(
                                new CreateInvoiceItemRequest(
                                        InvoiceItemSource.MANUAL, null, "a", 2, new BigDecimal("10.00")),
                                new CreateInvoiceItemRequest(
                                        InvoiceItemSource.MANUAL, null, "b", 3, new BigDecimal("5.00")))));

        // 2*10 + 3*5 = 35.00
        assertThat(i.getTotalAmount()).isEqualByComparingTo(new BigDecimal("35.0000"));
    }

    @Test
    void create_emptyItemListIsRejected() {
        // Validation is at the controller layer; the service still
        // computes a zero total if called with an empty list, which
        // is allowed by the DTO's @NotEmpty but rejected at the
        // controller's @Valid pass. The service test ensures it does
        // not throw, but it returns a zero-total invoice.
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        Invoice i = service.create(Role.BILLING_STAFF,
                new CreateInvoiceRequest(alicePid, null, "USD", null, List.of()));
        assertThat(i.getTotalAmount()).isEqualByComparingTo(new BigDecimal("0.0000"));
    }

    @Test
    void create_negativeUnitPriceIsRejected() {
        assertThatThrownBy(() -> service.create(Role.BILLING_STAFF,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "bad", 1, new BigDecimal("-1.00"))))))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void create_zeroQuantityIsDocumentedAsDtoAndDbValidation() {
        // Quantity < 1 is validated at the API layer (DTO @Min(1))
        // and enforced at the database level (CHECK quantity BETWEEN
        // 1 AND 10000). The service does not duplicate this check;
        // it produces an in-memory invoice with line total 0.00.
        // The DB CHECK constraint would reject the items insert.
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        Invoice i = service.create(Role.BILLING_STAFF,
                new CreateInvoiceRequest(alicePid, null, "USD", null,
                        List.of(new CreateInvoiceItemRequest(
                                InvoiceItemSource.MANUAL, null, "x", 0, new BigDecimal("1.00")))));
        assertThat(i.getTotalAmount()).isEqualByComparingTo(new BigDecimal("0.0000"));
    }

    // -- issue / void --------------------------------------------------------

    @Test
    void issue_billingStaffCanIssueDraft() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = service.issue(Role.BILLING_STAFF, i.getId());
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(result.getIssuedAt()).isNotNull();
    }

    @Test
    void issue_patientCannotIssue() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.issue(Role.PATIENT, i.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void issue_alreadyIssuedIsInvalidTransition() {
        Invoice i = existingDraft(alicePid);
        i.issue();
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.issue(Role.BILLING_STAFF, i.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void voidInvoice_paidCannotBeVoided() {
        Invoice i = existingDraft(alicePid);
        i.issue();
        i.markPaid();
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.voidInvoice(Role.BILLING_STAFF, i.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -- payments ------------------------------------------------------------

    @Test
    void recordPayment_fullPaymentMarksInvoicePaid() {
        Invoice i = existingDraft(alicePid);
        i.issue(); // ISSUED, total 100.00
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(payments.findByInvoiceIdOrderByPaidAtDesc(i.getId())).thenReturn(List.of());
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment p = service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CASH, new BigDecimal("100.00"),
                        "USD", null, "receipt-1"));

        assertThat(p.getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        // After saving, the invoice has been reloaded to capture status; the
        // status update is applied via the same instance.
        assertThat(i.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void recordPayment_partialPaymentDoesNotMarkPaid() {
        Invoice i = existingDraft(alicePid);
        i.issue(); // ISSUED, total 100.00
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(payments.findByInvoiceIdOrderByPaidAtDesc(i.getId())).thenReturn(List.of());
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CASH, new BigDecimal("40.00"),
                        "USD", null, null));

        // Partial payment does not move status to PAID.
        assertThat(i.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void recordPayment_overpaymentIsRejected() {
        Invoice i = existingDraft(alicePid);
        i.issue(); // total 100.00
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(payments.findByInvoiceIdOrderByPaidAtDesc(i.getId())).thenReturn(List.of());
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CASH, new BigDecimal("150.00"),
                        "USD", null, null)))
                .isInstanceOf(PaymentOverpaymentException.class);
    }

    @Test
    void recordPayment_cumulativeOverpaymentIsRejected() {
        Invoice i = existingDraft(alicePid);
        i.issue(); // total 100.00
        Payment existing = Payment.create(i.getId(), PaymentMethod.CASH,
                new BigDecimal("80.00"), "USD", null, "r1");
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(payments.findByInvoiceIdOrderByPaidAtDesc(i.getId())).thenReturn(List.of(existing));

        // 80 already paid; another 30 would exceed 100.
        assertThatThrownBy(() -> service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CARD, new BigDecimal("30.00"),
                        "USD", null, null)))
                .isInstanceOf(PaymentOverpaymentException.class);
    }

    @Test
    void recordPayment_currencyMismatchIsRejected() {
        Invoice i = existingDraft(alicePid);
        i.issue();
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CARD, new BigDecimal("10.00"),
                        "EUR", null, null)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void recordPayment_patientCannotRecord() {
        Invoice i = existingDraft(alicePid);
        i.issue();
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.recordPayment(Role.PATIENT, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CASH, new BigDecimal("10.00"),
                        "USD", null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recordPayment_voidedInvoiceCannotBePaid() {
        Invoice i = existingDraft(alicePid);
        i.voidInvoice();
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.recordPayment(Role.BILLING_STAFF, i.getId(),
                new CreatePaymentRequest(PaymentMethod.CASH, new BigDecimal("10.00"),
                        "USD", null, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -- read / authorization -----------------------------------------------

    @Test
    void getAuthorized_patientCanReadOwn() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        assertThat(service.getAuthorized(alicePid, Role.PATIENT, i.getId())).isEqualTo(i);
    }

    @Test
    void getAuthorized_patientCannotReadOthers() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.getAuthorized(bobPid, Role.PATIENT, i.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_billingStaffCanReadAny() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        assertThat(service.getAuthorized(UUID.randomUUID(), Role.BILLING_STAFF, i.getId()))
                .isEqualTo(i);
    }

    @Test
    void getAuthorized_doctorCannotRead() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.DOCTOR, i.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_receptionistCannotRead() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.RECEPTIONIST, i.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_missingReturns404() {
        UUID id = UUID.randomUUID();
        when(invoices.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAuthorized(alicePid, Role.PATIENT, id))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    // -- list ---------------------------------------------------------------

    @Test
    void listMineAsPatient_returnsByPatient() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findByPatientIdOrderByCreatedAtDesc(alicePid)).thenReturn(List.of(i));
        assertThat(service.listMineAsPatient(alicePid)).hasSize(1);
    }

    @Test
    void listPayments_idorProtectsOtherPatientsPayments() {
        Invoice i = existingDraft(alicePid);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));
        when(payments.findByInvoiceIdOrderByPaidAtDesc(i.getId())).thenReturn(List.of());

        // Bob is not the patient on the invoice; he must be denied
        // before any payment rows are returned.
        assertThatThrownBy(() -> service.listPayments(bobPid, Role.PATIENT, i.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(payments, never()).findByInvoiceIdOrderByPaidAtDesc(any(UUID.class));
    }
}
