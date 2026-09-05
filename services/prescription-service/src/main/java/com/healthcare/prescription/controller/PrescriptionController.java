package com.healthcare.prescription.controller;

import com.healthcare.prescription.dto.request.CreatePrescriptionRequest;
import com.healthcare.prescription.dto.request.PrescriptionItemRequest;
import com.healthcare.prescription.dto.response.PrescriptionResponse;
import com.healthcare.prescription.entity.Prescription;
import com.healthcare.prescription.entity.PrescriptionItem;
import com.healthcare.prescription.exception.AccessDeniedException;
import com.healthcare.prescription.security.CurrentPrincipalService;
import com.healthcare.prescription.security.Role;
import com.healthcare.prescription.service.PrescriptionService;
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
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;
    private final CurrentPrincipalService current;

    public PrescriptionController(PrescriptionService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    /** DOCTOR — create a prescription under their own identity. */
    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest req) {
        Prescription p = service.create(current.currentUserId(), current.currentRole(), req);
        List<PrescriptionItem> its = service.listItems(current.currentUserId(), current.currentRole(), p.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PrescriptionResponse.from(p, its));
    }

    /** PATIENT — list own prescriptions. */
    @GetMapping("/patient/me")
    public List<PrescriptionResponse> myPrescriptions() {
        Role role = current.currentRole();
        if (role != Role.PATIENT) {
            throw new AccessDeniedException();
        }
        return service.listMineAsPatient(current.currentUserId())
                .stream().map(PrescriptionResponse::summaryOnly).toList();
    }

    /** DOCTOR — list own (issued) prescriptions. */
    @GetMapping("/doctor/me")
    public List<PrescriptionResponse> myIssued() {
        Role role = current.currentRole();
        if (role != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        return service.listMineAsDoctor(current.currentUserId())
                .stream().map(PrescriptionResponse::summaryOnly).toList();
    }

    @GetMapping("/{id}")
    public PrescriptionResponse getById(@PathVariable("id") UUID id) {
        Prescription p = service.getAuthorized(
                current.currentUserId(), current.currentRole(), id);
        List<PrescriptionItem> its = service.listItems(
                current.currentUserId(), current.currentRole(), id);
        return PrescriptionResponse.from(p, its);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<PrescriptionResponse.PrescriptionItemResponse> addItem(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PrescriptionItemRequest req) {
        PrescriptionItem item = service.addItem(
                current.currentUserId(), current.currentRole(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PrescriptionResponse.PrescriptionItemResponse.from(item));
    }

    @PatchMapping("/{id}/cancel")
    public PrescriptionResponse cancel(@PathVariable("id") UUID id) {
        Prescription p = service.cancel(current.currentUserId(), current.currentRole(), id);
        List<PrescriptionItem> its = service.listItems(
                current.currentUserId(), current.currentRole(), id);
        return PrescriptionResponse.from(p, its);
    }
}
