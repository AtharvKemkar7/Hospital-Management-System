package com.healthcare.doctor.controller;

import com.healthcare.doctor.dto.request.CreateDoctorRequest;
import com.healthcare.doctor.dto.request.UpdateDoctorRequest;
import com.healthcare.doctor.dto.response.DoctorResponse;
import com.healthcare.doctor.entity.Doctor;
import com.healthcare.doctor.security.CurrentPrincipalService;
import com.healthcare.doctor.service.DoctorService;
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
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService service;
    private final CurrentPrincipalService current;

    public DoctorController(DoctorService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    /** Self-registration: an authenticated DOCTOR creates their own profile. */
    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest req) {
        Doctor d = service.createForCurrentUser(
                current.currentUserId(), current.currentRole(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(DoctorResponse.from(d));
    }

    @GetMapping("/me")
    public DoctorResponse me() {
        return DoctorResponse.from(service.getMine(current.currentUserId()));
    }

    @PatchMapping("/me")
    public DoctorResponse updateMe(@Valid @RequestBody UpdateDoctorRequest req) {
        return DoctorResponse.from(service.updateMine(current.currentUserId(), req));
    }

    /**
     * Get a doctor by id. Per {@code docs/service-boundaries.md}, any
     * authenticated user can read a doctor profile. The service does
     * not expose another service's data.
     */
    @GetMapping("/{id}")
    public DoctorResponse getById(@PathVariable("id") UUID id) {
        return DoctorResponse.from(service.getById(id));
    }
}
