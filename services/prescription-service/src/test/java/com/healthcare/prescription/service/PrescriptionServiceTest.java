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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionServiceTest {

    private PrescriptionRepository prescriptions;
    private PrescriptionItemRepository items;
    private PrescriptionService service;

    private final UUID drSmith   = UUID.randomUUID();
    private final UUID drJones   = UUID.randomUUID();
    private final UUID alicePid  = UUID.randomUUID();
    private final UUID bobPid    = UUID.randomUUID();
    private final UUID aliceAppt = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        prescriptions = mock(PrescriptionRepository.class);
        items = mock(PrescriptionItemRepository.class);
        service = new PrescriptionService(prescriptions, items);
    }

    private Prescription existing(UUID doctorId, UUID patientId) {
        return Prescription.create(patientId, doctorId, aliceAppt, "initial");
    }

    // -- create -------------------------------------------------------------

    @Test
    void create_doctorCanCreateUnderOwnIdentity() {
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        Prescription p = service.create(drSmith, Role.DOCTOR,
                new CreatePrescriptionRequest(
                        alicePid, aliceAppt, "first prescription", List.of()));

        assertThat(p.getDoctorId()).isEqualTo(drSmith); // forced, not from body
        assertThat(p.getPatientId()).isEqualTo(alicePid);
        assertThat(p.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void create_doctorCanProvideItemsInline() {
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.save(any(PrescriptionItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Prescription p = service.create(drSmith, Role.DOCTOR,
                new CreatePrescriptionRequest(
                        alicePid, aliceAppt, "with items",
                        List.of(new PrescriptionItemRequest(
                                "Amoxicillin", "500 mg", "every 8 hours",
                                "oral", 7, 21, "after meals"))));

        assertThat(p.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        verify(items).save(any(PrescriptionItem.class));
    }

    @Test
    void create_patientCannotCreate() {
        assertThatThrownBy(() -> service.create(alicePid, Role.PATIENT,
                new CreatePrescriptionRequest(alicePid, aliceAppt, "self", List.of())))
                .isInstanceOf(AccessDeniedException.class);
        verify(prescriptions, never()).save(any());
    }

    @Test
    void create_receptionistCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.RECEPTIONIST,
                new CreatePrescriptionRequest(alicePid, aliceAppt, null, List.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_billingStaffCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.BILLING_STAFF,
                new CreatePrescriptionRequest(alicePid, aliceAppt, null, List.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_adminCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.ADMIN,
                new CreatePrescriptionRequest(alicePid, aliceAppt, null, List.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -- read / authorization -----------------------------------------------

    @Test
    void getAuthorized_patientCanReadOwn() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThat(service.getAuthorized(alicePid, Role.PATIENT, p.getId())).isEqualTo(p);
    }

    @Test
    void getAuthorized_patientCannotReadOthers() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getAuthorized(bobPid, Role.PATIENT, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_prescribingDoctorCanRead() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThat(service.getAuthorized(drSmith, Role.DOCTOR, p.getId())).isEqualTo(p);
    }

    @Test
    void getAuthorized_otherDoctorCannotRead() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getAuthorized(drJones, Role.DOCTOR, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_adminCanReadAny() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThat(service.getAuthorized(UUID.randomUUID(), Role.ADMIN, p.getId()))
                .isEqualTo(p);
    }

    @Test
    void getAuthorized_billingStaffCannotRead() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.BILLING_STAFF, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_receptionistCannotRead() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.RECEPTIONIST, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_missingReturns404() {
        UUID id = UUID.randomUUID();
        when(prescriptions.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthorized(alicePid, Role.PATIENT, id))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    // -- items ---------------------------------------------------------------

    @Test
    void addItem_prescribingDoctorCanAddWhileIssued() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));
        when(items.save(any(PrescriptionItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItem item = service.addItem(drSmith, Role.DOCTOR, p.getId(),
                new PrescriptionItemRequest("Ibuprofen", "400 mg", "every 6 hours",
                        "oral", 5, 20, "with food"));

        assertThat(item.getPrescriptionId()).isEqualTo(p.getId());
    }

    @Test
    void addItem_otherDoctorCannotAdd() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.addItem(drJones, Role.DOCTOR, p.getId(),
                new PrescriptionItemRequest("X", "1 mg", "qd", null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(items, never()).save(any());
    }

    @Test
    void addItem_patientCannotAdd() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.addItem(alicePid, Role.PATIENT, p.getId(),
                new PrescriptionItemRequest("X", "1 mg", "qd", null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addItem_cannotAddAfterCancelled() {
        Prescription p = existing(drSmith, alicePid);
        p.cancel();
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.addItem(drSmith, Role.DOCTOR, p.getId(),
                new PrescriptionItemRequest("X", "1 mg", "qd", null, null, null, null)))
                .isInstanceOf(PrescriptionNotEditableException.class);
    }

    // -- cancel --------------------------------------------------------------

    @Test
    void cancel_prescribingDoctorCanCancel() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.cancel(drSmith, Role.DOCTOR, p.getId()).getStatus())
                .isEqualTo(PrescriptionStatus.CANCELLED);
    }

    @Test
    void cancel_otherDoctorCannotCancel() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.cancel(drJones, Role.DOCTOR, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_alreadyCancelledIsInvalidTransition() {
        Prescription p = existing(drSmith, alicePid);
        p.cancel();
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.cancel(drSmith, Role.DOCTOR, p.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancel_patientCannotCancel() {
        Prescription p = existing(drSmith, alicePid);
        when(prescriptions.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.cancel(alicePid, Role.PATIENT, p.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -- list ----------------------------------------------------------------

    @Test
    void listMineAsPatient_returnsByPatient() {
        Prescription a = existing(drSmith, alicePid);
        when(prescriptions.findByPatientIdOrderByIssuedAtDesc(alicePid))
                .thenReturn(List.of(a));
        assertThat(service.listMineAsPatient(alicePid)).hasSize(1);
    }

    @Test
    void listMineAsDoctor_returnsByDoctor() {
        Prescription a = existing(drSmith, alicePid);
        when(prescriptions.findByDoctorIdOrderByIssuedAtDesc(drSmith))
                .thenReturn(List.of(a));
        assertThat(service.listMineAsDoctor(drSmith)).hasSize(1);
    }
}
