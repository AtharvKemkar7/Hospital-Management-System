package com.healthcare.patient.repository;

import com.healthcare.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
