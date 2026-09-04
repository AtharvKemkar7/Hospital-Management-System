package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.request.CancelAppointmentRequest;
import com.healthcare.appointment.dto.request.CreateAppointmentRequest;
import com.healthcare.appointment.dto.request.RescheduleAppointmentRequest;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.AppointmentStatus;
import com.healthcare.appointment.entity.AppointmentType;
import com.healthcare.appointment.exception.AccessDeniedException;
import com.healthcare.appointment.exception.AppointmentNotFoundException;
import com.healthcare.appointment.exception.AppointmentSlotConflictException;
import com.healthcare.appointment.exception.InvalidAppointmentTimeException;
import com.healthcare.appointment.exception.InvalidStatusTransitionException;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {

    private AppointmentRepository repo;
    private AppointmentService service;

    private final UUID aliceUserId = UUID.randomUUID();
    private final UUID bobUserId   = UUID.randomUUID();
    private final UUID drSmith     = UUID.randomUUID();
    private final UUID drJones     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(AppointmentRepository.class);
        service = new AppointmentService(repo);
    }

    // -- helpers -------------------------------------------------------------

    private Instant inHours(int n) {
        return Instant.now().plusSeconds(n * 3600L);
    }

    private Appointment existing(UUID patientId, UUID doctorId, AppointmentStatus status) {
        Appointment a = Appointment.create(patientId, doctorId,
                inHours(2), inHours(3),
                AppointmentType.IN_PERSON, "checkup", patientId);
        switch (status) {
            case REQUESTED -> { /* default */ }
            case CONFIRMED -> a.confirm();
            case CANCELLED -> a.cancel("test");
            case COMPLETED -> { a.confirm(); a.complete(); }
            case NO_SHOW -> { a.confirm(); a.markNoShow(); }
        }
        return a;
    }

    // -- create --------------------------------------------------------------

    @Test
    void create_patientBooksOwnAppointment() {
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment a = service.create(aliceUserId, Role.PATIENT,
                new CreateAppointmentRequest(aliceUserId, drSmith,
                        inHours(2), inHours(3),
                        AppointmentType.IN_PERSON, "annual checkup"));

        assertThat(a.getPatientId()).isEqualTo(aliceUserId);
        assertThat(a.getDoctorId()).isEqualTo(drSmith);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        verify(repo).save(any(Appointment.class));
    }

    @Test
    void create_patientBodyPatientIdIsIgnoredAndForcedToCaller() {
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // A patient supplies bobUserId in the body; the service must
        // silently force it to aliceUserId. This is a safe-by-default
        // behavior: a patient can never book for someone else even by
        // trying. The body field is ignored.
        Appointment a = service.create(aliceUserId, Role.PATIENT,
                new CreateAppointmentRequest(bobUserId, drSmith,
                        inHours(2), inHours(3),
                        null, null));

        assertThat(a.getPatientId()).isEqualTo(aliceUserId);
        assertThat(a.getPatientId()).isNotEqualTo(bobUserId);
    }

    @Test
    void create_receptionistCanBookForAnyPatient() {
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID receptionistId = UUID.randomUUID();
        Appointment a = service.create(receptionistId, Role.RECEPTIONIST,
                new CreateAppointmentRequest(bobUserId, drSmith,
                        inHours(2), inHours(3), null, null));

        assertThat(a.getPatientId()).isEqualTo(bobUserId);
        assertThat(a.getCreatedBy()).isEqualTo(receptionistId);
    }

    @Test
    void create_doctorCannotBook() {
        assertThatThrownBy(() -> service.create(drSmith, Role.DOCTOR,
                new CreateAppointmentRequest(aliceUserId, drSmith,
                        inHours(2), inHours(3), null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_duplicateSlotIsRejected() {
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.create(aliceUserId, Role.PATIENT,
                new CreateAppointmentRequest(aliceUserId, drSmith,
                        inHours(2), inHours(3), null, null)))
                .isInstanceOf(AppointmentSlotConflictException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_raceLostOnSlotIsRejected() {
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("uq_appointments_doctor_slot_active"));

        assertThatThrownBy(() -> service.create(aliceUserId, Role.PATIENT,
                new CreateAppointmentRequest(aliceUserId, drSmith,
                        inHours(2), inHours(3), null, null)))
                .isInstanceOf(AppointmentSlotConflictException.class);
    }

    @Test
    void create_invalidTimeRangeIsRejected() {
        assertThatThrownBy(() -> service.create(aliceUserId, Role.PATIENT,
                new CreateAppointmentRequest(aliceUserId, drSmith,
                        inHours(3), inHours(2), null, null)))
                .isInstanceOf(InvalidAppointmentTimeException.class);
    }

    // -- read ----------------------------------------------------------------

    @Test
    void getMineAsPatient_returnsOwn() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findByIdAndPatientId(a.getId(), aliceUserId)).thenReturn(Optional.of(a));

        assertThat(service.getMineAsPatient(aliceUserId, a.getId())).isEqualTo(a);
    }

    @Test
    void getMineAsPatient_otherPatientReturnsNotFound() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findByIdAndPatientId(a.getId(), bobUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMineAsPatient(bobUserId, a.getId()))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void getByIdAuthorized_patientCannotReadOthers() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.getByIdAuthorized(a.getId(), bobUserId, Role.PATIENT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByIdAuthorized_doctorCannotReadOthers() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.getByIdAuthorized(a.getId(), drJones, Role.DOCTOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByIdAuthorized_assignedDoctorCanRead() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThat(service.getByIdAuthorized(a.getId(), drSmith, Role.DOCTOR)).isEqualTo(a);
    }

    // -- confirm / complete / cancel ----------------------------------------

    @Test
    void confirm_doctorCanConfirmOwn() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.confirm(drSmith, Role.DOCTOR, a.getId());
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void confirm_otherDoctorCannotConfirm() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.confirm(drJones, Role.DOCTOR, a.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirm_invalidTransition() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.COMPLETED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.confirm(drSmith, Role.DOCTOR, a.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancel_ownerCanCancel() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.cancel(aliceUserId, Role.PATIENT, a.getId(),
                new CancelAppointmentRequest("changed my mind"));
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.getCancelledReason()).isEqualTo("changed my mind");
    }

    @Test
    void cancel_completedCannotBeCancelled() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.COMPLETED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.cancel(aliceUserId, Role.PATIENT, a.getId(),
                new CancelAppointmentRequest("late")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void complete_assignedDoctorCanComplete() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.CONFIRMED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.complete(drSmith, Role.DOCTOR, a.getId()).getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void complete_otherDoctorCannotComplete() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.CONFIRMED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.complete(drJones, Role.DOCTOR, a.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void complete_invalidFromRequested() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.complete(drSmith, Role.DOCTOR, a.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -- reschedule ----------------------------------------------------------

    @Test
    void reschedule_ownerCancelsAndCreatesNew() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        when(repo.existsByDoctorIdAndStartAtAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment fresh = service.reschedule(aliceUserId, Role.PATIENT, a.getId(),
                new RescheduleAppointmentRequest(inHours(5), inHours(6), null));

        // The old row was cancelled, a new REQUESTED row was returned.
        assertThat(fresh.getId()).isNotEqualTo(a.getId());
        assertThat(fresh.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        // Two saves: the cancel of the old row, then the insert of the new row.
        verify(repo, org.mockito.Mockito.times(2)).save(any(Appointment.class));
    }

    @Test
    void reschedule_otherPatientCannot() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.reschedule(bobUserId, Role.PATIENT, a.getId(),
                new RescheduleAppointmentRequest(inHours(5), inHours(6), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMineAsDoctor_returnsOwn() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findByIdAndDoctorId(a.getId(), drSmith)).thenReturn(Optional.of(a));

        assertThat(service.getMineAsDoctor(drSmith, a.getId())).isEqualTo(a);
    }

    @Test
    void getByIdAuthorized_adminCanRead() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThat(service.getByIdAuthorized(a.getId(), UUID.randomUUID(), Role.ADMIN))
                .isEqualTo(a);
    }

    @Test
    void getByIdAuthorized_billingStaffCannotRead() {
        Appointment a = existing(aliceUserId, drSmith, AppointmentStatus.REQUESTED);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.getByIdAuthorized(a.getId(), UUID.randomUUID(),
                Role.BILLING_STAFF))
                .isInstanceOf(AccessDeniedException.class);
    }
}
