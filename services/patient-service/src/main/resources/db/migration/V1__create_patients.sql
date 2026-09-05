-- V1: Create patients table.
-- Patient Service owns this table exclusively. No cross-service FKs.
-- The link to Auth Service identity is the `user_id` UUID; it is NOT a
-- database foreign key (database-per-service isolation).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE patients (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid         NOT NULL,
    first_name      text         NOT NULL,
    last_name       text         NOT NULL,
    date_of_birth   date,
    gender          text,
    phone           text,
    email           text,
    status          text         NOT NULL DEFAULT 'PENDING',
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_patients_user_id UNIQUE (user_id),
    CONSTRAINT ck_patients_gender  CHECK (gender IS NULL OR gender IN
        ('FEMALE','MALE','OTHER','UNSPECIFIED')),
    CONSTRAINT ck_patients_status  CHECK (status IN
        ('PENDING','ACTIVE','INACTIVE','DECEASED'))
);

CREATE INDEX idx_patients_status     ON patients(status);
CREATE INDEX idx_patients_last_name  ON patients(last_name);
CREATE INDEX idx_patients_created_at ON patients(created_at);
