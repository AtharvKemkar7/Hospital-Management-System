package com.healthcare.doctor.service;

import com.healthcare.doctor.dto.request.CreateDoctorRequest;
import com.healthcare.doctor.dto.request.UpdateDoctorRequest;
import com.healthcare.doctor.entity.Doctor;
import com.healthcare.doctor.exception.AccessDeniedException;
import com.healthcare.doctor.exception.DoctorAlreadyExistsException;
import com.healthcare.doctor.exception.DoctorNotFoundException;
import com.healthcare.doctor.repository.DoctorRepository;
import com.healthcare.doctor.security.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for the doctor professional profile.
 *
 * <p>Authorization model:
 * <ul>
 *   <li>A {@code DOCTOR} can read or modify their own profile (matched on
 *       {@code userId} from the JWT).</li>
 *   <li>An {@code ADMIN} can read any profile and update any profile.</li>
 *   <li>Any other role calling a non-self endpoint is rejected.</li>
 * </ul>
 */
@Service
public class DoctorService {

    private final DoctorRepository doctors;

    public DoctorService(DoctorRepository doctors) {
        this.doctors = doctors;
    }

    @Transactional
    public Doctor createForCurrentUser(UUID currentUserId, Role currentRole,
                                       CreateDoctorRequest req) {
        if (currentRole != Role.DOCTOR) {
            throw new AccessDeniedException();
        }
        if (doctors.existsByUserId(currentUserId)) {
            throw new DoctorAlreadyExistsException();
        }
        if (doctors.existsByLicenseNumber(req.licenseNumber().trim())) {
            throw new DoctorAlreadyExistsException();
        }
        Doctor d = Doctor.create(
                currentUserId,
                req.firstName(),
                req.lastName(),
                req.licenseNumber(),
                req.specialty(),
                req.subSpecialty(),
                req.department(),
                req.phone(),
                req.email()
        );
        try {
            return doctors.save(d);
        } catch (DataIntegrityViolationException e) {
            // Race with a concurrent request or with the (deferred) UserRegistered consumer.
            throw new DoctorAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public Doctor getMine(UUID currentUserId) {
        return doctors.findByUserId(currentUserId)
                .orElseThrow(DoctorNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Doctor getById(UUID doctorId) {
        return doctors.findById(doctorId)
                .orElseThrow(DoctorNotFoundException::new);
    }

    @Transactional
    public Doctor updateMine(UUID currentUserId, UpdateDoctorRequest req) {
        Doctor d = doctors.findByUserId(currentUserId)
                .orElseThrow(DoctorNotFoundException::new);
        // If the license number is changing, ensure no other doctor already has it.
        if (req.licenseNumber() != null
                && !req.licenseNumber().trim().equals(d.getLicenseNumber())
                && doctors.existsByLicenseNumber(req.licenseNumber().trim())) {
            throw new DoctorAlreadyExistsException();
        }
        d.applyUpdate(
                req.firstName(),
                req.lastName(),
                req.licenseNumber(),
                req.specialty(),
                req.subSpecialty(),
                req.department(),
                req.phone(),
                req.email()
        );
        return doctors.save(d);
    }
}
