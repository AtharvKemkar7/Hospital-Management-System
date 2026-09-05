package com.healthcare.medicalrecord.repository;

import com.healthcare.medicalrecord.entity.Vital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VitalRepository extends JpaRepository<Vital, UUID> {

    List<Vital> findByRecordId(UUID recordId);
}
