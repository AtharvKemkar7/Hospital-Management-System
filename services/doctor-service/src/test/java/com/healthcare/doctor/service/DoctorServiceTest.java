package com.healthcare.doctor.service;

import com.healthcare.doctor.dto.request.CreateDoctorRequest;
import com.healthcare.doctor.dto.request.UpdateDoctorRequest;
import com.healthcare.doctor.entity.Doctor;
import com.healthcare.doctor.entity.DoctorStatus;
import com.healthcare.doctor.exception.AccessDeniedException;
import com.healthcare.doctor.exception.DoctorAlreadyExistsException;
import com.healthcare.doctor.exception.DoctorNotFoundException;
import com.healthcare.doctor.repository.DoctorRepository;
import com.healthcare.doctor.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorServiceTest {

    private DoctorRepository repo;
    private DoctorService service;

    private final UUID aliceDoctorId = UUID.randomUUID();
    private final UUID bobDoctorId   = UUID.randomUUID();
    private final String aliceLicense = "MD-12345";

    @BeforeEach
    void setUp() {
        repo = mock(DoctorRepository.class);
        service = new DoctorService(repo);
    }

    @Test
    void createForCurrentUser_persistsActiveProfile() {
        when(repo.existsByUserId(aliceDoctorId)).thenReturn(false);
        when(repo.existsByLicenseNumber(aliceLicense)).thenReturn(false);
        when(repo.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        Doctor d = service.createForCurrentUser(aliceDoctorId, Role.DOCTOR,
                new CreateDoctorRequest("Alice", "Smith", aliceLicense,
                        "Cardiology", "Heart Failure", "Cardiac Dept",
                        "+10000000000", "alice@hospital.com"));

        assertThat(d.getId()).isNotNull();
        assertThat(d.getUserId()).isEqualTo(aliceDoctorId);
        assertThat(d.getStatus()).isEqualTo(DoctorStatus.ACTIVE);
        assertThat(d.getFirstName()).isEqualTo("Alice");
        assertThat(d.getLicenseNumber()).isEqualTo(aliceLicense);
        assertThat(d.getSpecialty()).isEqualTo("Cardiology");
        verify(repo).save(any(Doctor.class));
    }

    @Test
    void createForCurrentUser_duplicateUserIsRejected() {
        when(repo.existsByUserId(aliceDoctorId)).thenReturn(true);

        assertThatThrownBy(() -> service.createForCurrentUser(aliceDoctorId, Role.DOCTOR,
                new CreateDoctorRequest("A", "B", aliceLicense, "Spec", null, null, null, null)))
                .isInstanceOf(DoctorAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createForCurrentUser_duplicateLicenseIsRejected() {
        when(repo.existsByUserId(aliceDoctorId)).thenReturn(false);
        when(repo.existsByLicenseNumber(aliceLicense)).thenReturn(true);

        assertThatThrownBy(() -> service.createForCurrentUser(aliceDoctorId, Role.DOCTOR,
                new CreateDoctorRequest("A", "B", aliceLicense, "Spec", null, null, null, null)))
                .isInstanceOf(DoctorAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createForCurrentUser_nonDoctorIsForbidden() {
        assertThatThrownBy(() -> service.createForCurrentUser(aliceDoctorId, Role.PATIENT,
                new CreateDoctorRequest("A", "B", "LIC-1", "Spec", null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void getMine_missingProfileReturnsNotFound() {
        when(repo.findByUserId(aliceDoctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(aliceDoctorId))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void getById_missingReturnsNotFound() {
        when(repo.findById(aliceDoctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(aliceDoctorId))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void updateMine_appliesNonNullFields() {
        Doctor existing = Doctor.create(aliceDoctorId, "Alice", "Smith", aliceLicense,
                "Cardiology", null, null, "111", "alice@x.com");
        when(repo.findByUserId(aliceDoctorId)).thenReturn(Optional.of(existing));
        when(repo.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        Doctor updated = service.updateMine(aliceDoctorId,
                new UpdateDoctorRequest("Alicia", null, null, "Neurology",
                        null, "Neuro Dept", "222", null));

        assertThat(updated.getFirstName()).isEqualTo("Alicia");
        assertThat(updated.getLastName()).isEqualTo("Smith");      // unchanged
        assertThat(updated.getSpecialty()).isEqualTo("Neurology");
        assertThat(updated.getDepartment()).isEqualTo("Neuro Dept");
        assertThat(updated.getPhone()).isEqualTo("222");
        assertThat(updated.getLicenseNumber()).isEqualTo(aliceLicense); // unchanged
    }

    @Test
    void updateMine_changingLicenseToExistingOneIsRejected() {
        Doctor existing = Doctor.create(aliceDoctorId, "Alice", "Smith", aliceLicense,
                "Cardiology", null, null, null, null);
        when(repo.findByUserId(aliceDoctorId)).thenReturn(Optional.of(existing));
        when(repo.existsByLicenseNumber("MD-99999")).thenReturn(true);

        assertThatThrownBy(() -> service.updateMine(aliceDoctorId,
                new UpdateDoctorRequest(null, null, "MD-99999", null, null, null, null, null)))
                .isInstanceOf(DoctorAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateMine_keepingSameLicenseIsAllowed() {
        Doctor existing = Doctor.create(aliceDoctorId, "Alice", "Smith", aliceLicense,
                "Cardiology", null, null, null, null);
        when(repo.findByUserId(aliceDoctorId)).thenReturn(Optional.of(existing));
        when(repo.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        Doctor updated = service.updateMine(aliceDoctorId,
                new UpdateDoctorRequest(null, null, aliceLicense, null, null, null, null, null));

        assertThat(updated.getLicenseNumber()).isEqualTo(aliceLicense);
        verify(repo).save(any(Doctor.class));
    }
}
