package com.healthcare.appointment.dto.request;

import com.healthcare.appointment.entity.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/appointments}. The authenticated
 * caller's role determines the {@code patientId} resolution:
 * <ul>
 *   <li>{@code PATIENT} — the patient is the caller; {@code patientId}
 *       is taken from the JWT (any supplied value is ignored).</li>
 *   <li>{@code RECEPTIONIST} — {@code patientId} is required in the
 *       body and is validated to be a non-null UUID.</li>
 * </ul>
 * In Phase 4, existence of the patient and doctor is not verified
 * via cross-service REST (no Redis, no synchronous call contract
 * yet). This limitation is documented; see the service class.
 */
public record CreateAppointmentRequest(

        @NotNull
        UUID patientId,

        @NotNull
        UUID doctorId,

        @NotNull
        @Future
        Instant startAt,

        @NotNull
        @Future
        Instant endAt,

        AppointmentType type,

        @Size(max = 1000)
        String reason
) { }
