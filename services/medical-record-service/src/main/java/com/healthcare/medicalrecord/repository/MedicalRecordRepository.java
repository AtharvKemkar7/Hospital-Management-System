package com.healthcare.medicalrecord.repository;

import com.healthcare.medicalrecord.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    List<MedicalRecord> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    List<MedicalRecord> findByDoctorIdOrderByRecordedAtDesc(UUID doctorId);
}
