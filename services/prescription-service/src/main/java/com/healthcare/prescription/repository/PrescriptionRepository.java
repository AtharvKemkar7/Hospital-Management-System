package com.healthcare.prescription.repository;

import com.healthcare.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByPatientIdOrderByIssuedAtDesc(UUID patientId);

    List<Prescription> findByDoctorIdOrderByIssuedAtDesc(UUID doctorId);
}
