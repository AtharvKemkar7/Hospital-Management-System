package com.healthcare.prescription.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/prescriptions}.
 *
 * <p>The {@code doctorId} is <b>not</b> taken from the body: it is
 * forced to the authenticated DOCTOR's {@code userId} (from the JWT)
 * in the service layer. A doctor can only create prescriptions under
 * their own identity.
 *
 * <p>Items may be supplied inline so the entire prescription (header
 * + items) is created atomically. Items can also be added later via
 * {@code POST /{id}/items} while the prescription is {@code ISSUED}.
 *
 * <p>Cross-service existence validation (does this patient exist?
 * was this appointment completed?) is <b>deferred</b>. The
 * Prescription Service accepts the supplied IDs at face value,
 * matching the pattern established in the Appointment and Medical
 * Record services.
 */
public record CreatePrescriptionRequest(

        @NotNull
        UUID patientId,

        @NotNull
        UUID appointmentId,

        @Size(max = 2000)
        String notes,

        List<PrescriptionItemRequest> items
) { }
