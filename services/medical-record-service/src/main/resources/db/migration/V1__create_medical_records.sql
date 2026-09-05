-- V1: Create medical_records, diagnoses, and vitals tables.
-- Medical Record Service owns these tables exclusively. No cross-service
-- FKs. The link to Patient Service, Doctor Service, and Appointment
-- Service identities is via UUID columns without foreign keys.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- medical_records
-- Clinical-record aggregate. Append-only by domain design: there is no
-- PATCH endpoint. The record is created once and is extended through
-- `diagnoses` and `vitals` sub-records.
-- ---------------------------------------------------------------------------
CREATE TABLE medical_records (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      uuid         NOT NULL,
    doctor_id       uuid         NOT NULL,
    appointment_id  uuid         NOT NULL,
    recorded_at     timestamptz  NOT NULL DEFAULT now(),
    summary         text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

-- Indexes support the documented access patterns:
--   GET by id (rarely)
--   list by patient_id (PATIENT me, ADMIN queries)
--   list by doctor_id (DOCTOR queries)
--   list by appointment_id (back-references)
CREATE INDEX idx_medical_records_patient_id     ON medical_records(patient_id);
CREATE INDEX idx_medical_records_doctor_id      ON medical_records(doctor_id);
CREATE INDEX idx_medical_records_appointment_id ON medical_records(appointment_id);
CREATE INDEX idx_medical_records_recorded_at    ON medical_records(recorded_at);

-- ---------------------------------------------------------------------------
-- diagnoses
-- Per-record ICD-10-coded diagnosis entries. One record can carry many.
-- ICD-10 format: letter, two digits, optional dot, optional 1-4 alphanumerics.
-- E.g. "I10", "E11.9", "S72.001A".
-- ---------------------------------------------------------------------------
CREATE TABLE diagnoses (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id    uuid         NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    icd10_code   text         NOT NULL,
    description  text         NOT NULL,
    onset_date   date,
    created_at   timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_diagnoses_icd10_format
        CHECK (icd10_code ~ '^[A-Z][0-9]{2}(\.[0-9A-Z]{1,4})?$')
);

CREATE INDEX idx_diagnoses_record_id  ON diagnoses(record_id);
CREATE INDEX idx_diagnoses_icd10_code ON diagnoses(icd10_code);

-- ---------------------------------------------------------------------------
-- vitals
-- Per-record vital-signs readings.
-- ---------------------------------------------------------------------------
CREATE TABLE vitals (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id       uuid         NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    taken_at        timestamptz  NOT NULL DEFAULT now(),
    systolic        integer,
    diastolic       integer,
    heart_rate      integer,
    temperature_c   numeric(4,1),
    spo2            integer,
    created_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_vitals_systolic   CHECK (systolic   IS NULL OR (systolic   BETWEEN 30 AND 300)),
    CONSTRAINT ck_vitals_diastolic  CHECK (diastolic  IS NULL OR (diastolic  BETWEEN 20 AND 200)),
    CONSTRAINT ck_vitals_heart_rate CHECK (heart_rate IS NULL OR (heart_rate BETWEEN 20 AND 250)),
    CONSTRAINT ck_vitals_spo2       CHECK (spo2       IS NULL OR (spo2       BETWEEN 50 AND 100))
);

CREATE INDEX idx_vitals_record_id ON vitals(record_id);
CREATE INDEX idx_vitals_taken_at  ON vitals(taken_at);
