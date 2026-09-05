package com.healthcare.medicalrecord.controller;

import com.healthcare.medicalrecord.dto.request.CreateDiagnosisRequest;
import com.healthcare.medicalrecord.dto.request.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.request.CreateVitalRequest;
import com.healthcare.medicalrecord.dto.response.MedicalRecordResponse;
import com.healthcare.medicalrecord.entity.Diagnosis;
import com.healthcare.medicalrecord.entity.MedicalRecord;
import com.healthcare.medicalrecord.entity.Vital;
import com.healthcare.medicalrecord.exception.AccessDeniedException;
import com.healthcare.medicalrecord.security.CurrentPrincipalService;
import com.healthcare.medicalrecord.security.Role;
import com.healthcare.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;
    private final CurrentPrincipalService current;

    public MedicalRecordController(MedicalRecordService service, CurrentPrincipalService current) {
        this.service = service;
        this.current = current;
    }

    /** DOCTOR — create a record under their own identity. */
    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody CreateMedicalRecordRequest req) {
        MedicalRecord r = service.create(current.currentUserId(), current.currentRole(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicalRecordResponse.summaryOnly(r));
    }

    /**
     * PATIENT — list own records.
     * The docs list {@code GET /api/v1/medical-records?patientId=...} as
     * a generic list, but explicitly say "no unrestricted patient
     * search". We expose only {@code /patient/me} to keep the surface
     * narrow and the authorization explicit.
     */
    @GetMapping("/patient/me")
    public List<MedicalRecordResponse> myRecords() {
        Role role = current.currentRole();
        if (role != Role.PATIENT) {
            throw new AccessDeniedException();
        }
        return service.listMineAsPatient(current.currentUserId())
                .stream().map(MedicalRecordResponse::summaryOnly).toList();
    }

    @GetMapping("/{id}")
    public MedicalRecordResponse getById(@PathVariable("id") UUID id) {
        MedicalRecord r = service.getAuthorized(
                current.currentUserId(), current.currentRole(), id);
        return MedicalRecordResponse.from(
                r,
                service.listDiagnoses(current.currentUserId(), current.currentRole(), id),
                service.listVitals(current.currentUserId(), current.currentRole(), id)
        );
    }

    @PostMapping("/{id}/diagnoses")
    public ResponseEntity<MedicalRecordResponse.DiagnosisResponse> addDiagnosis(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateDiagnosisRequest req) {
        Diagnosis d = service.addDiagnosis(
                current.currentUserId(), current.currentRole(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MedicalRecordResponse.DiagnosisResponse.from(d));
    }

    @PostMapping("/{id}/vitals")
    public ResponseEntity<MedicalRecordResponse.VitalResponse> addVital(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateVitalRequest req) {
        Vital v = service.addVital(
                current.currentUserId(), current.currentRole(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MedicalRecordResponse.VitalResponse.from(v));
    }
}
