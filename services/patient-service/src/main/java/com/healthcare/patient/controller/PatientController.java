package com.healthcare.patient.controller;

import com.healthcare.patient.dto.request.CreatePatientRequest;
import com.healthcare.patient.dto.request.UpdatePatientRequest;
import com.healthcare.patient.dto.response.PatientResponse;
import com.healthcare.patient.entity.Patient;
import com.healthcare.patient.security.CurrentPrincipalService;
import com.healthcare.patient.service.PatientService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService service;
    private final CurrentPrincipalService current;

    public PatientController(PatientService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    /** Self-registration: an authenticated PATIENT creates their own profile. */
    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody CreatePatientRequest req) {
        Patient p = service.createForCurrentUser(
                current.currentUserId(), current.currentRole(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponse.from(p));
    }

    @GetMapping("/me")
    public PatientResponse me() {
        return PatientResponse.from(service.getMine(current.currentUserId()));
    }

    @PatchMapping("/me")
    public PatientResponse updateMe(@Valid @RequestBody UpdatePatientRequest req) {
        return PatientResponse.from(service.updateMine(current.currentUserId(), req));
    }

    /**
     * Get a patient by id. The {@link PatientService} enforces that
     * a PATIENT can only access their own profile; an ADMIN can access
     * any profile; other roles are rejected.
     */
    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable("id") UUID id) {
        Patient p = service.getByIdAuthorized(id, current.currentUserId(), current.currentRole());
        return PatientResponse.from(p);
    }
}
