-- V1: Create users and refresh_tokens tables.
-- Auth Service owns these tables exclusively. No cross-service FKs.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                   uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                text         NOT NULL,
    password_hash        text         NOT NULL,
    first_name           text         NOT NULL,
    last_name            text         NOT NULL,
    role                 text         NOT NULL,
    status               text         NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified       boolean      NOT NULL DEFAULT false,
    failed_login_count   integer      NOT NULL DEFAULT 0,
    locked_until         timestamptz,
    last_login_at        timestamptz,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email                   UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase         CHECK (email = lower(email)),
    CONSTRAINT ck_users_role                    CHECK (role IN
        ('PATIENT','DOCTOR','ADMIN','RECEPTIONIST','BILLING_STAFF')),
    CONSTRAINT ck_users_status                  CHECK (status IN
        ('PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED','DEACTIVATED')),
    CONSTRAINT ck_users_failed_login_count      CHECK (failed_login_count >= 0)
);

CREATE INDEX idx_users_status     ON users(status);
CREATE INDEX idx_users_role       ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

-- ---------------------------------------------------------------------------
-- refresh_tokens
-- Only the SHA-256 hash of the token is stored. The raw token is returned
-- to the client once and never persisted.
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    text         NOT NULL,
    issued_at     timestamptz  NOT NULL DEFAULT now(),
    expires_at    timestamptz  NOT NULL,
    revoked_at    timestamptz,
    replaced_by   uuid         REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    user_agent    text,
    ip            text,
    created_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_tokens_token_hash      UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_expires_after   CHECK (expires_at > issued_at)
);

-- Primary lookup path during refresh: find a non-revoked, non-expired token by its hash.
CREATE INDEX idx_refresh_tokens_user_id     ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_revoked_at  ON refresh_tokens(revoked_at);

-- ---------------------------------------------------------------------------
-- audit_log
-- Per-service audit trail. Phase 1 only writes entries from the Auth
-- Service itself (auth events, role changes, lockouts).
-- ---------------------------------------------------------------------------
CREATE TABLE audit_log (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid         REFERENCES users(id) ON DELETE SET NULL,
    actor_user_id uuid        REFERENCES users(id) ON DELETE SET NULL,
    action       text         NOT NULL,
    target       text,
    ip           text,
    user_agent   text,
    created_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_user_id    ON audit_log(user_id);
CREATE INDEX idx_audit_log_action     ON audit_log(action);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
