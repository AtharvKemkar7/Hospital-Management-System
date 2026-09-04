package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.request.CancelAppointmentRequest;
import com.healthcare.appointment.dto.request.CreateAppointmentRequest;
import com.healthcare.appointment.dto.request.RescheduleAppointmentRequest;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.AppointmentStatus;
import com.healthcare.appointment.exception.AccessDeniedException;
import com.healthcare.appointment.exception.AppointmentNotFoundException;
import com.healthcare.appointment.exception.AppointmentSlotConflictException;
import com.healthcare.appointment.exception.InvalidAppointmentTimeException;
import com.healthcare.appointment.exception.InvalidStatusTransitionException;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.security.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Appointment lifecycle and authorization logic.
 *
 * <p>Authorization model (per {@code docs/service-boundaries.md} §5):
 * <ul>
 *   <li>{@code PATIENT} — may create an appointment only for their own
 *       {@code userId} (the body's {@code patientId} is ignored and
 *       forced to the caller). May cancel their own. May read their own.</li>
 *   <li>{@code DOCTOR} — may read/confirm/complete appointments where
 *       they are the doctor. May cancel their own. May reschedule their own.</li>
 *   <li>{@code RECEPTIONIST} — may create for any patient, may read all,
 *       may cancel/reschedule any.</li>
 *   <li>{@code ADMIN} — may read all; may complete any; other transitions
 *       are not allowed by this service in Phase 4.</li>
 *   <li>Other roles (e.g. {@code BILLING_STAFF}) are denied.</li>
 * </ul>
 *
 * <p>Cross-service validation (does this patient exist? is this doctor
 * active?) is <b>deferred</b> for Phase 4. The architecture specifies
 * "REST + short Redis cache" validation at booking time, but Redis
 * is not yet available and a synchronous cross-service call contract
 * is not yet established. This is documented as a known limitation.
 */
@Service
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;

    public AppointmentService(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public Appointment create(UUID currentUserId, Role currentRole,
                              CreateAppointmentRequest req) {
        validateTimeRange(req.startAt(), req.endAt());

        UUID patientId = resolvePatientId(currentUserId, currentRole, req.patientId());

        if (currentRole == Role.DOCTOR || currentRole == Role.BILLING_STAFF) {
            throw new AccessDeniedException();
        }
        if (currentRole == Role.PATIENT && !patientId.equals(currentUserId)) {
            // A patient may not create an appointment for someone else.
            throw new AccessDeniedException();
        }
        if (currentRole != Role.PATIENT && currentRole != Role.RECEPTIONIST && currentRole != Role.ADMIN) {
            throw new AccessDeniedException();
        }

        // Service-layer pre-check (for a clearer error message); the
        // database partial unique index is the final concurrency guard.
        if (appointments.existsByDoctorIdAndStartAtAndStatusIn(
                req.doctorId(), req.startAt(), ACTIVE_STATUSES)) {
            throw new AppointmentSlotConflictException();
        }

        Appointment a = Appointment.create(
                patientId, req.doctorId(),
                req.startAt(), req.endAt(),
                req.type(), req.reason(),
                currentUserId);

        try {
            return appointments.save(a);
        } catch (DataIntegrityViolationException e) {
            // Race lost: another transaction inserted the same slot first.
            throw new AppointmentSlotConflictException();
        }
    }

    private UUID resolvePatientId(UUID currentUserId, Role currentRole, UUID requested) {
        if (currentRole == Role.PATIENT) {
            // Always the caller; ignore anything in the body.
            return currentUserId;
        }
        // RECEPTIONIST / ADMIN: caller-supplied id is authoritative here.
        if (requested == null) {
            throw new InvalidAppointmentTimeException("patientId is required for this role");
        }
        return requested;
    }

    private void validateTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new InvalidAppointmentTimeException("startAt and endAt are required");
        }
        if (!end.isAfter(start)) {
            throw new InvalidAppointmentTimeException("endAt must be after startAt");
        }
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public Appointment getMineAsPatient(UUID currentUserId, UUID appointmentId) {
        return appointments.findByIdAndPatientId(appointmentId, currentUserId)
                .orElseThrow(AppointmentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Appointment> listMineAsPatient(UUID currentUserId) {
        return appointments.findByPatientIdOrderByStartAtDesc(currentUserId);
    }

    @Transactional(readOnly = true)
    public Appointment getMineAsDoctor(UUID currentUserId, UUID appointmentId) {
        return appointments.findByIdAndDoctorId(appointmentId, currentUserId)
                .orElseThrow(AppointmentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Appointment> listMineAsDoctor(UUID currentUserId) {
        return appointments.findByDoctorIdOrderByStartAtDesc(currentUserId);
    }

    @Transactional(readOnly = true)
    public Appointment getByIdAuthorized(UUID appointmentId,
                                        UUID currentUserId, Role currentRole) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        authorizeRead(a, currentUserId, currentRole);
        return a;
    }

    private void authorizeRead(Appointment a, UUID currentUserId, Role currentRole) {
        switch (currentRole) {
            case ADMIN, RECEPTIONIST -> { /* full read */ }
            case PATIENT -> {
                if (!a.getPatientId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            case DOCTOR -> {
                if (!a.getDoctorId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            default -> throw new AccessDeniedException();
        }
    }

    // -------------------------------------------------------------- transitions

    @Transactional
    public Appointment confirm(UUID currentUserId, Role currentRole, UUID appointmentId) {
        if (currentRole != Role.DOCTOR && currentRole != Role.RECEPTIONIST) {
            throw new AccessDeniedException();
        }
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (currentRole == Role.DOCTOR && !a.getDoctorId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        if (a.getStatus() != AppointmentStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(a.getStatus().name(), "CONFIRMED");
        }
        a.confirm();
        return appointments.save(a);
    }

    @Transactional
    public Appointment cancel(UUID currentUserId, Role currentRole, UUID appointmentId,
                              CancelAppointmentRequest req) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        authorizeTransition(a, currentUserId, currentRole, "CANCELLED");
        if (a.getStatus().isTerminal()) {
            throw new InvalidStatusTransitionException(a.getStatus().name(), "CANCELLED");
        }
        a.cancel(req == null ? null : req.reason());
        return appointments.save(a);
    }

    @Transactional
    public Appointment complete(UUID currentUserId, Role currentRole, UUID appointmentId) {
        if (currentRole != Role.DOCTOR && currentRole != Role.ADMIN) {
            throw new AccessDeniedException();
        }
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (currentRole == Role.DOCTOR && !a.getDoctorId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        if (a.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(a.getStatus().name(), "COMPLETED");
        }
        a.complete();
        return appointments.save(a);
    }

    /**
     * Reschedule = mark the current row as CANCELLED (reason "rescheduled")
     * and create a new REQUESTED appointment at the new time. The
     * service-layer and DB-level slot-uniqueness check still applies
     * to the new row.
     */
    @Transactional
    public Appointment reschedule(UUID currentUserId, Role currentRole, UUID appointmentId,
                                  RescheduleAppointmentRequest req) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
        if (currentRole == Role.PATIENT && !a.getPatientId().equals(currentUserId)) {
            throw new AccessDeniedException();
        }
        if (currentRole != Role.PATIENT
                && currentRole != Role.RECEPTIONIST) {
            throw new AccessDeniedException();
        }
        if (a.getStatus().isTerminal()) {
            throw new InvalidStatusTransitionException(a.getStatus().name(), "REQUESTED (reschedule)");
        }
        validateTimeRange(req.startAt(), req.endAt());

        a.cancel("rescheduled");
        appointments.save(a);

        if (appointments.existsByDoctorIdAndStartAtAndStatusIn(
                a.getDoctorId(), req.startAt(), ACTIVE_STATUSES)) {
            throw new AppointmentSlotConflictException();
        }
        Appointment fresh = Appointment.create(
                a.getPatientId(), a.getDoctorId(),
                req.startAt(), req.endAt(),
                a.getType(),
                req.reason() == null ? a.getReason() : req.reason(),
                currentUserId);
        try {
            return appointments.save(fresh);
        } catch (DataIntegrityViolationException e) {
            throw new AppointmentSlotConflictException();
        }
    }

    private void authorizeTransition(Appointment a, UUID currentUserId,
                                     Role currentRole, String target) {
        switch (currentRole) {
            case PATIENT -> {
                if (!a.getPatientId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            case DOCTOR -> {
                if (!a.getDoctorId().equals(currentUserId)) {
                    throw new AccessDeniedException();
                }
            }
            case RECEPTIONIST, ADMIN -> { /* allowed */ }
            default -> throw new AccessDeniedException();
        }
    }
}
