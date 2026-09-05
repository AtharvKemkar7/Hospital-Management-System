package com.healthcare.prescription.service;

import com.healthcare.prescription.dto.request.CreatePrescriptionRequest;
import com.healthcare.prescription.dto.request.PrescriptionItemRequest;
import com.healthcare.prescription.entity.Prescription;
import com.healthcare.prescription.entity.PrescriptionItem;
import com.healthcare.prescription.entity.PrescriptionStatus;
import com.healthcare.prescription.exception.AccessDeniedException;
import com.healthcare.prescription.exception.InvalidStatusTransitionException;
import com.healthcare.prescription.exception.PrescriptionNotEditableException;
import com.healthcare.prescription.exception.PrescriptionNotFoundException;
import com.healthcare.prescription.repository.PrescriptionItemRepository;
import com.healthcare.prescription.repository.PrescriptionRepository;
import com.healthcare.prescription.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic and authorization for the Prescription Service.
 *
 * <p><b>Authorization model</b> (per {@code docs/service-boundaries.md} §7):
 * <ul>
 *   <li>{@code DOCTOR} — may create a prescription <b>under their own
 *       identity only</b> ({@code doctorId} is forced to the JWT's
 *       {@code userId}; the body field is ignored). May add items to
 *       prescriptions they created while the prescription is
 *       {@code ISSUED}. May cancel prescriptions they created.</li>
 *   <li>{@code PATIENT} — may read their own prescriptions. No
 *       create / modify / cancel.</li>
 *   <li>{@code ADMIN} — may read any prescription. "With audit" is
 *       documented but deferred (no audit table implemented in
 *       Phase 6).</li>
 *   <li>{@code RECEPTIONIST}, {@code BILLING_STAFF} — denied
 *       everywhere. Default to denial; the docs do not grant either
 *       role clinical access to prescriptions.</li>
 * </ul>
 *
 * <p>Cross-service existence validation (does this patient exist?
 * was this appointment completed with this doctor?) is
 * <b>deferred</b>. No REST client is created. The doctor identity is
 * trusted from the JWT (the safest available source); patient and
 * appointment ids are trusted from the body pending an inter-service
 * contract. This matches the pattern in Appointment and Medical
 * Record services.
 */
@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;

    public PrescriptionService(PrescriptionRepository prescriptions,
                               PrescriptionItemRepository items) {
        this.prescriptions = prescriptions;
        this.items = items;
    }

    // ------------------------------------------------------------------ create

    /**
     * Create a new prescription (header) under the authenticated
     * doctor's identity. If items are supplied in the request, they
     * are created in the same transaction.
     */
    @Transactional
    public Prescription create(UUID currentUserId, Role currentRole,
                                CreatePrescriptionRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        Prescription p = Prescription.create(
                req.patientId(),
                currentUserId,             // forced, not from body
                req.appointmentId(),
                req.notes()
        );
        Prescription saved = prescriptions.save(p);

        if (req.items() != null) {
            for (PrescriptionItemRequest ir : req.items()) {
                PrescriptionItem item = PrescriptionItem.create(
                        saved.getId(),
                        ir.drugName(), ir.dosage(), ir.frequency(),
                        ir.route(), ir.durationDays(), ir.quantity(),
                        ir.instructions());
                items.save(item);
            }
        }
        return saved;
    }

    /**
     * Add a single medication item to an existing prescription.
     * Only allowed when the prescription is {@code ISSUED} and the
     * caller is the prescribing doctor.
     */
    @Transactional
    public PrescriptionItem addItem(UUID currentUserId, Role currentRole,
                                    UUID prescriptionId,
                                    PrescriptionItemRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        Prescription p = loadOrThrow(prescriptionId);
        if (!p.getDoctorId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        if (!p.getStatus().canAcceptItems()) {
            throw new PrescriptionNotEditableException();
        }
        PrescriptionItem item = PrescriptionItem.create(
                p.getId(),
                req.drugName(), req.dosage(), req.frequency(),
                req.route(), req.durationDays(), req.quantity(),
                req.instructions());
        return items.save(item);
    }

    // ------------------------------------------------------------------ cancel

    @Transactional
    public Prescription cancel(UUID currentUserId, Role currentRole, UUID prescriptionId) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        Prescription p = loadOrThrow(prescriptionId);
        if (!p.getDoctorId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        if (p.getStatus() != PrescriptionStatus.ISSUED) {
            throw new InvalidStatusTransitionException(p.getStatus().name(), "CANCELLED");
        }
        p.cancel();
        return prescriptions.save(p);
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public Prescription getAuthorized(UUID currentUserId, Role currentRole, UUID prescriptionId) {
        Prescription p = loadOrThrow(prescriptionId);
        authorizeRead(p, currentUserId, currentRole);
        return p;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionItem> listItems(UUID currentUserId, Role currentRole,
                                            UUID prescriptionId) {
        // Authorize the parent first.
        getAuthorized(currentUserId, currentRole, prescriptionId);
        return items.findByPrescriptionId(prescriptionId);
    }

    @Transactional(readOnly = true)
    public List<Prescription> listMineAsPatient(UUID currentUserId) {
        return prescriptions.findByPatientIdOrderByIssuedAtDesc(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<Prescription> listMineAsDoctor(UUID currentUserId) {
        return prescriptions.findByDoctorIdOrderByIssuedAtDesc(currentUserId);
    }

    // ------------------------------------------------------------------ helpers

    private Prescription loadOrThrow(UUID prescriptionId) {
        return prescriptions.findById(prescriptionId)
                .orElseThrow(PrescriptionNotFoundException::new);
    }

    /**
     * Object-level authorization. The database record is the source
     * of truth: we never trust identity fields from the request body.
     */
    private void authorizeRead(Prescription p, UUID currentUserId, Role currentRole) {
        switch (currentRole) {
            case ADMIN -> { /* full read; "with audit" deferred */ }
            case PATIENT -> {
                if (!p.getPatientId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            case DOCTOR -> {
                // "DOCTOR (own)" per docs. The full "DOCTOR assigned to
                // this patient" check would require a cross-service
                // call to appointment-service; that contract is not
                // yet established. Conservatively: only the prescribing
                // doctor may read.
                if (!p.getDoctorId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            default -> throw new AccessDeniedException(); // RECEPTIONIST, BILLING_STAFF, ...
        }
    }
}
