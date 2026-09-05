package com.healthcare.medicalrecord.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body of {@code POST /api/v1/medical-records}.
 *
 * <p>The {@code doctorId} is <b>not</b> taken from the body: it is
 * forced to the authenticated DOCTOR's {@code userId} (from the JWT)
 * in the service layer. A doctor can only create a record under
 * their own identity.
 *
 * <p>{@code patientId} and {@code appointmentId} are accepted as
 * supplied. Cross-service existence validation is deferred (no
 * REST call to Patient / Doctor / Appointment Service is established
 * in this phase). The appointment relationship is documented in
 * {@code docs/service-boundaries.md} §6 but cannot be fully verified
 * here without a synchronous cross-service call.
 */
public record CreateMedicalRecordRequest(

        @NotNull
        UUID patientId,

        @NotNull
        UUID appointmentId,

        @Size(max = 4000)
        String summary
) { }
