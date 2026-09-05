package com.healthcare.patient.service;

import com.healthcare.patient.dto.request.CreatePatientRequest;
import com.healthcare.patient.dto.request.UpdatePatientRequest;
import com.healthcare.patient.entity.Patient;
import com.healthcare.patient.exception.AccessDeniedException;
import com.healthcare.patient.exception.PatientAlreadyExistsException;
import com.healthcare.patient.exception.PatientNotFoundException;
import com.healthcare.patient.repository.PatientRepository;
import com.healthcare.patient.security.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for the patient profile.
 *
 * <p>Authorization model:
 * <ul>
 *   <li>A {@code PATIENT} can only read or modify their own profile
 *       (matched on {@code userId} from the JWT).</li>
 *   <li>An {@code ADMIN} can read any profile and update any profile.</li>
 *   <li>All other roles are rejected with 403 FORBIDDEN.</li>
 * </ul>
 */
@Service
public class PatientService {

    private final PatientRepository patients;

    public PatientService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional
    public Patient createForCurrentUser(UUID currentUserId, Role currentRole,
                                        CreatePatientRequest req) {
        // Only a PATIENT may self-register their own profile in this endpoint.
        if (currentRole != Role.PATIENT) {
            throw new AccessDeniedException();
        }
        if (patients.existsByUserId(currentUserId)) {
            throw new PatientAlreadyExistsException();
        }
        Patient p = Patient.create(
                currentUserId,
                req.firstName(),
                req.lastName(),
                req.dateOfBirth(),
                req.gender(),
                req.phone(),
                req.email()
        );
        try {
            return patients.save(p);
        } catch (DataIntegrityViolationException e) {
            // Race with the (deferred) UserRegistered consumer or a concurrent request.
            throw new PatientAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public Patient getByIdAuthorized(UUID patientId, UUID currentUserId, Role currentRole) {
        Patient p = patients.findById(patientId)
                .orElseThrow(PatientNotFoundException::new);
        enforceOwnerOrAdmin(p, currentUserId, currentRole);
        return p;
    }

    @Transactional(readOnly = true)
    public Patient getMine(UUID currentUserId) {
        return patients.findByUserId(currentUserId)
                .orElseThrow(PatientNotFoundException::new);
    }

    @Transactional
    public Patient updateMine(UUID currentUserId, UpdatePatientRequest req) {
        Patient p = patients.findByUserId(currentUserId)
                .orElseThrow(PatientNotFoundException::new);
        p.applyUpdate(
                req.firstName(),
                req.lastName(),
                req.dateOfBirth(),
                req.gender(),
                req.phone(),
                req.email()
        );
        return patients.save(p);
    }

    private void enforceOwnerOrAdmin(Patient p, UUID currentUserId, Role currentRole) {
        if (currentRole == Role.ADMIN) {
            return;
        }
        if (currentRole == Role.PATIENT && p.getUserId().equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException();
    }
}
