package com.healthcare.appointment.repository;

import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndPatientId(UUID id, UUID patientId);

    Optional<Appointment> findByIdAndDoctorId(UUID id, UUID doctorId);

    List<Appointment> findByPatientIdOrderByStartAtDesc(UUID patientId);

    List<Appointment> findByDoctorIdOrderByStartAtDesc(UUID doctorId);

    boolean existsByDoctorIdAndStartAtAndStatusIn(UUID doctorId,
                                                  Instant startAt,
                                                  List<AppointmentStatus> statuses);
}
