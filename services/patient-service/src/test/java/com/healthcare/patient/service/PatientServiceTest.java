package com.healthcare.patient.service;

import com.healthcare.patient.dto.request.CreatePatientRequest;
import com.healthcare.patient.dto.request.UpdatePatientRequest;
import com.healthcare.patient.entity.Gender;
import com.healthcare.patient.entity.Patient;
import com.healthcare.patient.exception.AccessDeniedException;
import com.healthcare.patient.exception.PatientAlreadyExistsException;
import com.healthcare.patient.exception.PatientNotFoundException;
import com.healthcare.patient.repository.PatientRepository;
import com.healthcare.patient.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientServiceTest {

    private PatientRepository repo;
    private PatientService service;

    private final UUID aliceUserId = UUID.randomUUID();
    private final UUID bobUserId   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(PatientRepository.class);
        service = new PatientService(repo);
    }

    @Test
    void createForCurrentUser_persistsActiveProfile() {
        when(repo.existsByUserId(aliceUserId)).thenReturn(false);
        when(repo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient p = service.createForCurrentUser(
                aliceUserId, Role.PATIENT,
                new CreatePatientRequest("Alice", "Doe",
                        LocalDate.of(1990, 1, 1), Gender.FEMALE,
                        "+10000000000", "alice@example.com"));

        assertThat(p.getId()).isNotNull();
        assertThat(p.getUserId()).isEqualTo(aliceUserId);
        assertThat(p.getStatus()).isEqualTo(com.healthcare.patient.entity.PatientStatus.ACTIVE);
        assertThat(p.getFirstName()).isEqualTo("Alice");
        assertThat(p.getLastName()).isEqualTo("Doe");
        assertThat(p.getEmail()).isEqualTo("alice@example.com");
        verify(repo).save(any(Patient.class));
    }

    @Test
    void createForCurrentUser_duplicateIsRejected() {
        when(repo.existsByUserId(aliceUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.createForCurrentUser(aliceUserId, Role.PATIENT,
                new CreatePatientRequest("Alice", "Doe", null, null, null, null)))
                .isInstanceOf(PatientAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void createForCurrentUser_nonPatientIsForbidden() {
        assertThatThrownBy(() -> service.createForCurrentUser(aliceUserId, Role.ADMIN,
                new CreatePatientRequest("A", "B", null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void getByIdAuthorized_otherPatientIsForbidden() {
        UUID alicePatientId = UUID.randomUUID();
        Patient alice = Patient.create(aliceUserId, "Alice", "Doe", null, null, null, null);
        when(repo.findById(alicePatientId)).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> service.getByIdAuthorized(alicePatientId, bobUserId, Role.PATIENT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByIdAuthorized_adminCanReadAny() {
        UUID alicePatientId = UUID.randomUUID();
        Patient alice = Patient.create(aliceUserId, "Alice", "Doe", null, null, null, null);
        when(repo.findById(alicePatientId)).thenReturn(Optional.of(alice));

        Patient fetched = service.getByIdAuthorized(alicePatientId, bobUserId, Role.ADMIN);
        assertThat(fetched.getId()).isEqualTo(alice.getId());
    }

    @Test
    void getMine_missingProfileReturnsNotFound() {
        when(repo.findByUserId(aliceUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(aliceUserId))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void updateMine_appliesNonNullFields() {
        Patient existing = Patient.create(aliceUserId, "Alice", "Doe", null, Gender.FEMALE, "111", "alice@x.com");
        when(repo.findByUserId(aliceUserId)).thenReturn(Optional.of(existing));
        when(repo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient updated = service.updateMine(aliceUserId,
                new UpdatePatientRequest("Alicia", null, null, null, "222", null));

        assertThat(updated.getFirstName()).isEqualTo("Alicia");
        assertThat(updated.getLastName()).isEqualTo("Doe");
        assertThat(updated.getPhone()).isEqualTo("222");
        assertThat(updated.getEmail()).isEqualTo("alice@x.com");
    }
}
