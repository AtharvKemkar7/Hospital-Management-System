-- V1: Create invoices, invoice_items, and payments tables.
-- Billing Service owns these tables exclusively. No cross-service FKs.
-- The link to Patient, Doctor, and Appointment Service identities is via
-- UUID columns without foreign keys.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Monetary policy
--   column type : NUMERIC(19,4)
--   calculations: server-side, BigDecimal, RoundingMode.HALF_UP, scale 4
--   column type stores up to 4 fractional digits; calculations produce
--   scale 2 rounded values (e.g. 0.10 * 3 = 0.30).
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- invoices
-- ---------------------------------------------------------------------------
CREATE TABLE invoices (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      uuid         NOT NULL,
    appointment_id  uuid,
    status          text         NOT NULL DEFAULT 'DRAFT',
    total_amount    numeric(19,4) NOT NULL DEFAULT 0,
    currency        text         NOT NULL DEFAULT 'USD',
    issued_at       timestamptz,
    due_at          timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    -- optimistic locking
    version         bigint       NOT NULL DEFAULT 0,

    CONSTRAINT ck_invoices_status            CHECK (status IN ('DRAFT','ISSUED','PAID','VOID','REFUNDED')),
    CONSTRAINT ck_invoices_total_nonneg      CHECK (total_amount >= 0),
    CONSTRAINT ck_invoices_currency_format   CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_invoices_patient_id      ON invoices(patient_id);
CREATE INDEX idx_invoices_appointment_id  ON invoices(appointment_id);
CREATE INDEX idx_invoices_status          ON invoices(status);
CREATE INDEX idx_invoices_issued_at       ON invoices(issued_at);
CREATE INDEX idx_invoices_patient_status  ON invoices(patient_id, status);

-- ---------------------------------------------------------------------------
-- invoice_items
-- Source of the line: APPOINTMENT, PRESCRIPTION, MANUAL.
--   source_id is a UUID that points to the source row in the source service.
--   No FK — the source lives in another service's database.
-- ---------------------------------------------------------------------------
CREATE TABLE invoice_items (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    uuid         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    source        text         NOT NULL,
    source_id     uuid,
    description   text         NOT NULL,
    quantity      integer      NOT NULL,
    unit_price    numeric(19,4) NOT NULL,
    line_total    numeric(19,4) NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_invoice_items_source     CHECK (source IN ('APPOINTMENT','PRESCRIPTION','MANUAL')),
    CONSTRAINT ck_invoice_items_quantity   CHECK (quantity BETWEEN 1 AND 10000),
    CONSTRAINT ck_invoice_items_unit_price  CHECK (unit_price >= 0),
    CONSTRAINT ck_invoice_items_line_total  CHECK (line_total >= 0)
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_source     ON invoice_items(source, source_id);

-- ---------------------------------------------------------------------------
-- payments
-- A payment moves the invoice toward PAID. Idempotency is not yet
-- enforced (real payment provider not in scope).
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    uuid         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    method        text         NOT NULL,
    amount        numeric(19,4) NOT NULL,
    currency      text         NOT NULL,
    paid_at       timestamptz  NOT NULL DEFAULT now(),
    reference     text,
    created_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT ck_payments_method     CHECK (method IN ('CASH','CARD','INSURANCE','ONLINE')),
    CONSTRAINT ck_payments_amount     CHECK (amount > 0),
    CONSTRAINT ck_payments_currency    CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
CREATE INDEX idx_payments_paid_at     ON payments(paid_at);
