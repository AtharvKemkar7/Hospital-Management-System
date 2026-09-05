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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

class MedicalRecordServiceTest {

    private MedicalRecordRepository records;
    private DiagnosisRepository diagnoses;
    private VitalRepository vitals;
    private MedicalRecordService service;

    private final UUID drSmith = UUID.randomUUID();
    private final UUID drJones = UUID.randomUUID();
    private final UUID alicePatient = UUID.randomUUID();
    private final UUID bobPatient   = UUID.randomUUID();
    private final UUID aliceAppt    = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        records = mock(MedicalRecordRepository.class);
        diagnoses = mock(DiagnosisRepository.class);
        vitals = mock(VitalRepository.class);
        service = new MedicalRecordService(records, diagnoses, vitals);
    }

    private MedicalRecord existing(UUID doctorId, UUID patientId) {
        return MedicalRecord.create(patientId, doctorId, aliceAppt, "initial summary");
    }

    // -- creation -----------------------------------------------------------

    @Test
    void create_doctorCanCreateUnderOwnIdentity() {
        when(records.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord r = service.create(drSmith, Role.DOCTOR,
                new CreateMedicalRecordRequest(alicePatient, aliceAppt, "first visit"));

        assertThat(r.getDoctorId()).isEqualTo(drSmith); // forced, not from body
        assertThat(r.getPatientId()).isEqualTo(alicePatient);
        assertThat(r.getAppointmentId()).isEqualTo(aliceAppt);
    }

    @Test
    void create_patientCannotCreate() {
        assertThatThrownBy(() -> service.create(alicePatient, Role.PATIENT,
                new CreateMedicalRecordRequest(alicePatient, aliceAppt, "self-note")))
                .isInstanceOf(AccessDeniedException.class);
        verify(records, never()).save(any());
    }

    @Test
    void create_adminCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.ADMIN,
                new CreateMedicalRecordRequest(alicePatient, aliceAppt, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_billingStaffCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.BILLING_STAFF,
                new CreateMedicalRecordRequest(alicePatient, aliceAppt, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_receptionistCannotCreate() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Role.RECEPTIONIST,
                new CreateMedicalRecordRequest(alicePatient, aliceAppt, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -- read / authorization ----------------------------------------------

    @Test
    void getAuthorized_patientCanReadOwn() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThat(service.getAuthorized(alicePatient, Role.PATIENT, r.getId())).isEqualTo(r);
    }

    @Test
    void getAuthorized_patientCannotReadOthersRecord() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.getAuthorized(bobPatient, Role.PATIENT, r.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_doctorCannotReadAnotherDoctorsRecord() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.getAuthorized(drJones, Role.DOCTOR, r.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_adminCanReadAny() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThat(service.getAuthorized(UUID.randomUUID(), Role.ADMIN, r.getId())).isEqualTo(r);
    }

    @Test
    void getAuthorized_billingStaffCannotRead() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.BILLING_STAFF, r.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_receptionistCannotRead() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.getAuthorized(UUID.randomUUID(), Role.RECEPTIONIST, r.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAuthorized_missingRecordReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(records.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthorized(alicePatient, Role.PATIENT, id))
                .isInstanceOf(MedicalRecordNotFoundException.class);
    }

    // -- sub-records --------------------------------------------------------

    @Test
    void addDiagnosis_creatorDoctorCanAdd() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));
        when(diagnoses.save(any(Diagnosis.class))).thenAnswer(inv -> inv.getArgument(0));

        Diagnosis d = service.addDiagnosis(drSmith, Role.DOCTOR, r.getId(),
                new CreateDiagnosisRequest("I10", "Essential hypertension", LocalDate.of(2026, 1, 1)));

        assertThat(d.getIcd10Code()).isEqualTo("I10");
        assertThat(d.getRecordId()).isEqualTo(r.getId());
    }

    @Test
    void addDiagnosis_otherDoctorCannotAdd() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.addDiagnosis(drJones, Role.DOCTOR, r.getId(),
                new CreateDiagnosisRequest("I10", "x", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(diagnoses, never()).save(any());
    }

    @Test
    void addDiagnosis_patientCannotAdd() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.addDiagnosis(alicePatient, Role.PATIENT, r.getId(),
                new CreateDiagnosisRequest("I10", "x", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addVital_creatorDoctorCanAdd() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));
        when(vitals.save(any(Vital.class))).thenAnswer(inv -> inv.getArgument(0));

        Vital v = service.addVital(drSmith, Role.DOCTOR, r.getId(),
                new CreateVitalRequest(Instant.now(), 120, 80, 72, new BigDecimal("36.8"), 98));

        assertThat(v.getRecordId()).isEqualTo(r.getId());
        assertThat(v.getSystolic()).isEqualTo(120);
    }

    @Test
    void addVital_otherDoctorCannotAdd() {
        MedicalRecord r = existing(drSmith, alicePatient);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.addVital(drJones, Role.DOCTOR, r.getId(),
                new CreateVitalRequest(Instant.now(), 120, 80, 72, new BigDecimal("36.8"), 98)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listMineAsPatient_returnsByPatientId() {
        MedicalRecord a = existing(drSmith, alicePatient);
        MedicalRecord b = existing(drSmith, alicePatient);
        when(records.findByPatientIdOrderByRecordedAtDesc(alicePatient))
                .thenReturn(List.of(a, b));

        assertThat(service.listMineAsPatient(alicePatient)).hasSize(2);
    }

    @Test
    void listDiagnoses_authorizedCanList() {
        MedicalRecord r = existing(drSmith, alicePatient);
        Diagnosis d = Diagnosis.create(r.getId(), "I10", "htn", null);
        when(records.findById(r.getId())).thenReturn(Optional.of(r));
        when(diagnoses.findByRecordId(r.getId())).thenReturn(List.of(d));

        assertThat(service.listDiagnoses(alicePatient, Role.PATIENT, r.getId())).hasSize(1);
    }
}
