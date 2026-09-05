-- V1: Create prescriptions and prescription_items tables.
-- Prescription Service owns these tables exclusively. No cross-service
-- FKs. The link to Patient Service, Doctor Service, and Appointment
-- Service identities is via UUID columns without foreign keys.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- prescriptions
-- Aggregate for one clinical prescription. The status lifecycle is
-- documented in `docs/service-boundaries.md` §7:
--   ISSUED     -> CANCELLED | DISPENSED
--   CANCELLED  -> (terminal)
--   DISPENSED  -> (terminal)
-- ---------------------------------------------------------------------------
CREATE TABLE prescriptions (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      uuid         NOT NULL,
    doctor_id       uuid         NOT NULL,
    appointment_id  uuid         NOT NULL,
    issued_at       timestamptz  NOT NULL DEFAULT now(),
    status          text         NOT NULL DEFAULT 'ISSUED',
    notes           text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_prescriptions_status CHECK (status IN ('ISSUED','CANCELLED','DISPENSED'))
);

CREATE INDEX idx_prescriptions_patient_id     ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor_id      ON prescriptions(doctor_id);
CREATE INDEX idx_prescriptions_appointment_id ON prescriptions(appointment_id);
CREATE INDEX idx_prescriptions_issued_at      ON prescriptions(issued_at);
CREATE INDEX idx_prescriptions_status         ON prescriptions(status);
CREATE INDEX idx_prescriptions_patient_status ON prescriptions(patient_id, status);
CREATE INDEX idx_prescriptions_doctor_status  ON prescriptions(doctor_id, status);

-- ---------------------------------------------------------------------------
-- prescription_items
-- One prescription can carry one or more medication items. Items are
-- immutable once the parent prescription is no longer ISSUED.
-- ---------------------------------------------------------------------------
CREATE TABLE prescription_items (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id uuid         NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    drug_name       text         NOT NULL,
    dosage          text         NOT NULL,
    frequency       text         NOT NULL,
    route           text,
    duration_days   integer      CHECK (duration_days IS NULL OR duration_days BETWEEN 1 AND 365),
    quantity        integer      CHECK (quantity IS NULL OR quantity BETWEEN 1 AND 1000),
    instructions    text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_prescription_items_prescription_id ON prescription_items(prescription_id);
