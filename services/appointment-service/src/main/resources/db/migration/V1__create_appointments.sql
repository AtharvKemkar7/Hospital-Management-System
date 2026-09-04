-- V1: Create appointments table.
-- Appointment Service owns this table exclusively. No cross-service FKs.
-- The link to Patient Service and Doctor Service identities is via
-- `patient_id` and `doctor_id` UUIDs; they are NOT database foreign keys
-- (database-per-service isolation). See `docs/database-design.md` §7.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE appointments (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       uuid         NOT NULL,
    doctor_id        uuid         NOT NULL,
    start_at         timestamptz  NOT NULL,
    end_at           timestamptz  NOT NULL,
    type             text         NOT NULL DEFAULT 'IN_PERSON',
    reason           text,
    status           text         NOT NULL DEFAULT 'REQUESTED',
    created_by       uuid         NOT NULL,
    cancelled_reason text,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_appointments_type            CHECK (type IN ('IN_PERSON','TELEHEALTH')),
    CONSTRAINT ck_appointments_status          CHECK (status IN ('REQUESTED','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')),
    CONSTRAINT ck_appointments_time_order      CHECK (end_at > start_at)
);

-- Database-level protection against two appointments being scheduled for
-- the same doctor at the exact same start time. A partial unique index
-- ensures that only one non-terminal (REQUESTED/CONFIRMED) appointment
-- can exist for a given (doctor_id, start_at). CANCELLED and NO_SHOW rows
-- do not block the slot. COMPLETED rows are historical and likewise do
-- not block. This is the final concurrency protection; the service layer
-- also performs a pre-check for clearer error messages.
CREATE UNIQUE INDEX uq_appointments_doctor_slot_active
    ON appointments (doctor_id, start_at)
    WHERE status IN ('REQUESTED','CONFIRMED');

CREATE INDEX idx_appointments_patient_id     ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id      ON appointments(doctor_id);
CREATE INDEX idx_appointments_start_at       ON appointments(start_at);
CREATE INDEX idx_appointments_status         ON appointments(status);
CREATE INDEX idx_appointments_patient_status ON appointments(patient_id, status);
CREATE INDEX idx_appointments_doctor_status  ON appointments(doctor_id, status);
