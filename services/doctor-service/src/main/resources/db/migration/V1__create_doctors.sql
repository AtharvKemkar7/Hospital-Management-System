-- V1: Create doctors table.
-- Doctor Service owns this table exclusively. No cross-service FKs.
-- The link to Auth Service identity is the `user_id` UUID; it is NOT a
-- database foreign key (database-per-service isolation).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE doctors (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid         NOT NULL,
    first_name      text         NOT NULL,
    last_name       text         NOT NULL,
    license_number  text         NOT NULL,
    specialty       text         NOT NULL,
    sub_specialty   text,
    department      text,
    phone           text,
    email           text,
    status          text         NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_doctors_user_id        UNIQUE (user_id),
    CONSTRAINT uq_doctors_license_number UNIQUE (license_number),
    CONSTRAINT ck_doctors_status         CHECK (status IN ('ACTIVE','INACTIVE','ON_LEAVE'))
);

CREATE INDEX idx_doctors_specialty   ON doctors(specialty);
CREATE INDEX idx_doctors_last_name   ON doctors(last_name);
CREATE INDEX idx_doctors_department  ON doctors(department);
CREATE INDEX idx_doctors_status      ON doctors(status);
CREATE INDEX idx_doctors_created_at  ON doctors(created_at);
