package com.healthcare.billing.controller;

import com.healthcare.billing.dto.request.CreateInvoiceRequest;
import com.healthcare.billing.dto.request.CreatePaymentRequest;
import com.healthcare.billing.dto.response.InvoiceResponse;
import com.healthcare.billing.entity.Invoice;
import com.healthcare.billing.entity.InvoiceItem;
import com.healthcare.billing.entity.Payment;
import com.healthcare.billing.security.CurrentPrincipalService;
import com.healthcare.billing.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService service;
    private final CurrentPrincipalService current;

    public InvoiceController(InvoiceService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest req) {
        Invoice i = service.create(current.currentRole(), req);
        List<InvoiceItem> its = service.listItems(current.currentUserId(), current.currentRole(), i.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponse.from(i, its, null));
    }

    @GetMapping
    public List<InvoiceResponse> list() {
        // Per docs §8: BILLING_STAFF, ADMIN see all; PATIENT sees own.
        // DOCTOR and RECEPTIONIST are denied.
        var role = current.currentRole();
        if (role == com.healthcare.billing.security.Role.PATIENT) {
            return service.listMineAsPatient(current.currentUserId())
                    .stream().map(InvoiceResponse::summaryOnly).toList();
        }
        if (role == com.healthcare.billing.security.Role.BILLING_STAFF
                || role == com.healthcare.billing.security.Role.ADMIN) {
            return service.listAllForStaff()
                    .stream().map(InvoiceResponse::summaryOnly).toList();
        }
        // Defensive: the SecurityConfig requires authentication, but a
        // role not listed above (e.g. RECEPTIONIST, DOCTOR) must be
        // denied explicitly. Throw AccessDenied at this layer.
        throw new com.healthcare.billing.exception.AccessDeniedException();
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable("id") UUID id) {
        Invoice i = service.getAuthorized(
                current.currentUserId(), current.currentRole(), id);
        List<InvoiceItem> its = service.listItems(
                current.currentUserId(), current.currentRole(), id);
        List<Payment> pms = service.listPayments(
                current.currentUserId(), current.currentRole(), id);
        return InvoiceResponse.from(i, its, pms);
    }

    @PatchMapping("/{id}/issue")
    public InvoiceResponse issue(@PathVariable("id") UUID id) {
        Invoice i = service.issue(current.currentRole(), id);
        List<InvoiceItem> its = service.listItems(
                current.currentUserId(), current.currentRole(), id);
        return InvoiceResponse.from(i, its, null);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<InvoiceResponse.PaymentResponse> recordPayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreatePaymentRequest req) {
        Payment p = service.recordPayment(current.currentRole(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InvoiceResponse.PaymentResponse.from(p));
    }

    @GetMapping("/{id}/payments")
    public List<InvoiceResponse.PaymentResponse> listPayments(@PathVariable("id") UUID id) {
        return service.listPayments(
                current.currentUserId(), current.currentRole(), id)
                .stream().map(InvoiceResponse.PaymentResponse::from).toList();
    }
}
