package com.healthcare.medicalrecord.service;

import com.healthcare.medicalrecord.dto.request.CreateDiagnosisRequest;
import com.healthcare.medicalrecord.dto.request.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.request.CreateVitalRequest;
import com.healthcare.medicalrecord.entity.Diagnosis;
import com.healthcare.medicalrecord.entity.MedicalRecord;
import com.healthcare.medicalrecord.entity.Vital;
import com.healthcare.medicalrecord.exception.AccessDeniedException;
import com.healthcare.medicalrecord.exception.MedicalRecordNotFoundException;
import com.healthcare.medicalrecord.repository.DiagnosisRepository;
import com.healthcare.medicalrecord.repository.MedicalRecordRepository;
import com.healthcare.medicalrecord.repository.VitalRepository;
import com.healthcare.medicalrecord.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic and authorization for the Medical Record Service.
 *
 * <p><b>Authorization model</b> (per {@code docs/service-boundaries.md} §6):
 * <ul>
 *   <li>{@code DOCTOR} — may create a record <b>under their own
 *       identity only</b> ({@code doctorId} is forced to the JWT's
 *       {@code userId}; the body field is ignored). May add diagnoses
 *       and vitals to records they created.</li>
 *   <li>{@code PATIENT} — may read their own records
 *       ({@code patientId == JWT userId}). No create / no sub-record
 *       endpoints.</li>
 *   <li>{@code ADMIN} — may read any record. The "with audit" note in
 *       the docs is implemented as a deferred audit hook
 *       (see {@link #authorizeRead}).</li>
 *   <li>{@code RECEPTIONIST}, {@code BILLING_STAFF} — denied.
 *       Per the "Hard rule" in {@code docs/service-boundaries.md} §6
 *       these roles cannot read clinical content.</li>
 * </ul>
 *
 * <p>Cross-service existence validation (does this patient exist? is
 * this appointment with this doctor?) is <b>deferred</b> for Phase 5.
 * No REST call to Patient / Doctor / Appointment Service is made. The
 * doctor identity is trusted from the JWT (the safest available
 * source); patient and appointment ids are trusted from the body
 * pending an inter-service contract.
 */
@Service
public class MedicalRecordService {

    private final MedicalRecordRepository records;
    private final DiagnosisRepository diagnoses;
    private final VitalRepository vitals;

    public MedicalRecordService(MedicalRecordRepository records,
                                DiagnosisRepository diagnoses,
                                VitalRepository vitals) {
        this.records = records;
        this.diagnoses = diagnoses;
        this.vitals = vitals;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public MedicalRecord create(UUID currentUserId, Role currentRole,
                                CreateMedicalRecordRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        // doctorId is forced to the JWT's userId. The doctor can only
        // create records under their own identity.
        MedicalRecord r = MedicalRecord.create(
                req.patientId(),
                currentUserId,
                req.appointmentId(),
                req.summary()
        );
        return records.save(r);
    }

    @Transactional
    public Diagnosis addDiagnosis(UUID currentUserId, Role currentRole,
                                  UUID recordId, CreateDiagnosisRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        MedicalRecord r = loadRecordOrThrow(recordId);
        if (!r.getDoctorId().equals(currentUserId)) {
            // Only the doctor who created the record may add sub-records.
            throw new AccessDeniedException();
        }
        Diagnosis d = Diagnosis.create(
                r.getId(), req.icd10Code().toUpperCase(), req.description(), req.onsetDate());
        return diagnoses.save(d);
    }

    @Transactional
    public Vital addVital(UUID currentUserId, Role currentRole,
                          UUID recordId, CreateVitalRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        MedicalRecord r = loadRecordOrThrow(recordId);
        if (!r.getDoctorId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        Vital v = Vital.create(
                r.getId(), req.takenAt(),
                req.systolic(), req.diastolic(),
                req.heartRate(), req.temperatureC(), req.spo2());
        return vitals.save(v);
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public MedicalRecord getAuthorized(UUID currentUserId, Role currentRole, UUID recordId) {
        MedicalRecord r = loadRecordOrThrow(recordId);
        authorizeRead(r, currentUserId, currentRole);
        return r;
    }

    @Transactional(readOnly = true)
    public MedicalRecord getWithDetailsAuthorized(UUID currentUserId, Role currentRole,
                                                  UUID recordId) {
        MedicalRecord r = getAuthorized(currentUserId, currentRole, recordId);
        return r; // callers can fetch diagnoses/vitals via the same authorized boundary
    }

    @Transactional(readOnly = true)
    public List<Diagnosis> listDiagnoses(UUID currentUserId, Role currentRole, UUID recordId) {
        // Authorize against the record first; this enforces PATIENT/ADMIN/DOCTOR rules.
        getAuthorized(currentUserId, currentRole, recordId);
        return diagnoses.findByRecordId(recordId);
    }

    @Transactional(readOnly = true)
    public List<Vital> listVitals(UUID currentUserId, Role currentRole, UUID recordId) {
        getAuthorized(currentUserId, currentRole, recordId);
        return vitals.findByRecordId(recordId);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecord> listMineAsPatient(UUID currentUserId) {
        return records.findByPatientIdOrderByRecordedAtDesc(currentUserId);
    }

    // ------------------------------------------------------------------ helpers

    private MedicalRecord loadRecordOrThrow(UUID recordId) {
        return records.findById(recordId)
                .orElseThrow(MedicalRecordNotFoundException::new);
    }

    /**
     * Object-level authorization for medical-record reads.
     *
     * <p>The database record is the source of truth — we do not trust
     * any identity field from the request body. The caller is matched
     * against the record's actual {@code patientId} and
     * {@code doctorId}.
     */
    private void authorizeRead(MedicalRecord r, UUID currentUserId, Role currentRole) {
        switch (currentRole) {
            case ADMIN -> { /* full read; "with audit" deferred to a later phase */ }
            case PATIENT -> {
                if (!r.getPatientId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            case DOCTOR -> {
                // "DOCTOR (assigned to any appointment with this patient)"
                // is documented in service-boundaries §6. The assignment
                // check would require a cross-service call to
                // appointment-service to list the doctor's appointments;
                // that contract is not yet established. As a Phase 5
                // baseline, a doctor who created the record may read
                // it. Other doctors (not the creator) are denied.
                if (!r.getDoctorId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            default -> throw new AccessDeniedException(); // RECEPTIONIST, BILLING_STAFF, ...
        }
    }
}
