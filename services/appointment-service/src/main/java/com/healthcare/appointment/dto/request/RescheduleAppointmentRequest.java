package com.healthcare.appointment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body of {@code PATCH /api/v1/appointments/{id}/reschedule}. The
 * service cancels the existing appointment and creates a new one at
 * the requested time.
 */
public record RescheduleAppointmentRequest(

        @NotNull
        @Future
        Instant startAt,

        @NotNull
        @Future
        Instant endAt,

        @Size(max = 500)
        String reason
) { }
