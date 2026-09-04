# Auth Service (`auth-service`)

Healthcare Platform — Auth / Identity Service.

This is the **only** service that knows about user identities and issues
authentication tokens. It owns the `auth_db` database exclusively and
publishes user-lifecycle events on Kafka. Everything else in the platform
trusts the JWTs it issues.

- **Language / build:** Java 21 LTS, Spring Boot 3.3.x, Maven
- **Database:** PostgreSQL (`auth_db`)
- **Migrations:** Flyway (`src/main/resources/db/migration/V1__...sql`)
- **Cache:** Redis is **not** used by Phase 1
- **Messaging:** Kafka is **optional** for Phase 1 (publishes `UserRegistered` /
  `UserDeactivated` if a broker is configured; falls back to no-op otherwise)
- **Object storage:** not used by this service
- **Package:** `com.healthcare.auth`
- **Default port:** 8081 (override with `SERVER_PORT`)

---

## 1. Endpoints

All endpoints are versioned under `/api/v1/`.

| Method | Path                       | Auth   | Purpose                                  |
|--------|----------------------------|--------|------------------------------------------|
| POST   | `/api/v1/auth/register`    | public | Self-registration. Always creates a `PATIENT`. |
| POST   | `/api/v1/auth/login`       | public | Email + password. Issues access + refresh tokens. |
| POST   | `/api/v1/auth/refresh`     | public | Rotates the refresh token, returns a new pair. |
| POST   | `/api/v1/auth/logout`      | Bearer | Revokes the caller''s refresh tokens. |
| GET    | `/api/v1/auth/me`          | Bearer | Returns the authenticated user''s profile. |
| POST   | `/api/v1/users`            | Admin  | Admin-only user creation with explicit role. |
| GET    | `/actuator/health`         | public | Liveness + readiness groups. |
| GET    | `/actuator/info`           | public | Service info. |
| GET    | `/actuator/prometheus`     | denied  | Locked down in Phase 1. |

### Example: register

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json
X-Correlation-Id: <optional-uuid>

{
  "email": "alice@example.com",
  "password": "Sup3rSafe!Pass",
  "firstName": "Alice",
  "lastName": "Anderson",
  "role": "PATIENT"   <-- accepted but ignored: public registration always creates PATIENT
}
```

Response `201 Created`:

```json
{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9....",
  "refreshToken": "QFQ_LKqj2FNULxt-0aWmaNIQyEK0sFZcdhYh1YZw3Ak",
  "tokenType":    "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "id": "b7df9c2a-8597-4ad3-95c9-0c7855d15440",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Anderson",
    "role": "PATIENT",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2026-09-04T13:13:33.079Z",
    "createdAt":   "2026-09-04T13:13:33.062Z"
  }
}
```

### Error envelope

All error responses follow the foundation standard:

```json
{
  "timestamp":     "2026-09-04T13:13:44.456Z",
  "status":        400,
  "code":          "VALIDATION_ERROR",
  "message":       "Invalid request",
  "path":          "/api/v1/auth/register",
  "correlationId": "dcffb917-7d62-484b-b357-a3c950a6442b",
  "details": [
    { "field": "password", "issue": "password must be between 12 and 128 characters" }
  ]
}
```

Canonical error codes used by this service: `VALIDATION_ERROR`, `MALFORMED_JSON`,
`UNAUTHORIZED`, `TOKEN_EXPIRED`, `TOKEN_REVOKED`, `FORBIDDEN`, `CONFLICT`,
`INTERNAL_ERROR`.

---

## 2. Security design

- **Passwords:** BCrypt with cost 12 (configurable via `app.security.bcrypt-cost`).
- **JWT:** HS256, signed with `JWT_SECRET` (≥ 32 bytes in production — enforced at startup).
- **JWT claims (minimum necessary):** `sub`, `userId`, `role`, `iss`, `iat`, `exp`, `jti`. No email, no name, no PHI.
- **Refresh tokens:** 256-bit random opaque values, stored as SHA-256 hashes only.
  Single-use; rotation on every use; replay of a revoked token revokes **all** tokens for the user.
- **Account lockout:** 5 failed logins → account `LOCKED` for 15 minutes (configurable).
- **User-enumeration guard:** identical generic 401 message and identical response time on
  "no such user", "wrong password", "locked", and "not active".
- **Privilege escalation guard:** public registration **always** creates a `PATIENT`,
  regardless of the `role` field in the request body. Privileged accounts must be
  created by an authenticated `ADMIN` via `POST /api/v1/users` (Spring Security
  `@PreAuthorize("hasRole(''ADMIN'')")`).
- **No sensitive logging:** passwords, password hashes, JWTs, refresh tokens are
  never logged at any level.
- **CORS:** allow-list from configuration (`CORS_ALLOWED_ORIGINS`). No wildcards.

---

## 3. Configuration

All configuration comes from environment variables. Defaults for local dev are
in `application-local.yml`.

| Variable                                  | Default                                          | Notes |
|-------------------------------------------|--------------------------------------------------|-------|
| `SERVER_PORT`                             | `8081`                                           |       |
| `SPRING_PROFILES_ACTIVE`                  | `local`                                          | one of `local`, `dev`, `prod`, `test` |
| `DB_URL`                                  | `jdbc:postgresql://localhost:5432/auth_db`       | required in non-local profiles |
| `DB_USERNAME`                             | `auth`                                           | required in non-local profiles |
| `DB_PASSWORD`                             | `auth`                                           | required in non-local profiles |
| `JWT_SECRET`                              | local default only                               | **must** be ≥ 32 bytes in `prod` |
| `JWT_ISSUER`                              | `healthcare-platform`                            |       |
| `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS`     | `900`  (15 min)                                  |       |
| `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS`    | `1209600` (14 days)                              |       |
| `AUTH_EVENTS_ENABLED`                     | `false`                                          | set to `true` to publish Kafka events |
| `CORS_ALLOWED_ORIGINS`                    | `http://localhost:4200`                          | comma-separated list |

`SPRING_KAFKA_BOOTSTRAP_SERVERS` (not listed) controls the Kafka broker. When
`AUTH_EVENTS_ENABLED=false` or no broker is configured, `UserEventPublisher`
is a no-op and authentication still works.

---

## 4. Build & run

### Build

```bash
cd services/auth-service
mvn clean package
```

Produces `target/auth-service.jar` (executable Spring Boot fat JAR).

### Run locally against a local Postgres

```bash
docker run -d --name auth-pg -e POSTGRES_USER=auth -e POSTGRES_PASSWORD=auth \
    -e POSTGRES_DB=auth_db -p 5432:5432 postgres:16-alpine

DB_URL=jdbc:postgresql://localhost:5432/auth_db \
DB_USERNAME=auth DB_PASSWORD=auth \
JWT_SECRET=please-change-this-to-a-32-byte-or-longer-secret!! \
java -jar target/auth-service.jar
```

The Flyway migration in `V1__create_users_and_refresh_tokens.sql` is applied
automatically on startup. Hibernate `ddl-auto=validate` enforces that the JPA
entities match the database schema.

### Run tests

```bash
mvn test
```

19 unit + smoke tests. Comprehensive integration tests are deferred to a
dedicated testing phase.

---

## 5. Database

Database name: **`auth_db`**.

Tables:

- `users` — id (UUID), email (unique, lowercased), password_hash, first_name,
  last_name, role, status, email_verified, failed_login_count, locked_until,
  last_login_at, created_at, updated_at.
- `refresh_tokens` — id (UUID), user_id (FK), token_hash (unique, SHA-256),
  issued_at, expires_at, revoked_at, replaced_by, user_agent, ip, created_at.
- `audit_log` — id, user_id, actor_user_id, action, target, ip, user_agent, created_at.

No cross-service foreign keys. Cross-service references (none for Phase 1) would
be plain `uuid` columns with no DB-level FK.

---

## 6. Events

When `AUTH_EVENTS_ENABLED=true` and a Kafka broker is configured, the service
publishes to these topics:

- `healthcare.auth.UserRegistered` — `{userId, role, emailHint, occurredAt}`
  (email is **not** the raw value; it is a hint like `a***@example.com`).
- `healthcare.auth.UserDeactivated` — `{userId, role, occurredAt}`.

The envelope is the standard one defined in `docs/events.md`. If Kafka is
unreachable, authentication still succeeds; the failure is logged at WARN.

---

## 7. Out of scope (Phase 1)

These are documented in the foundation and will be addressed in later phases:

- Email verification flow (the `PENDING_VERIFICATION` status column is already
  in the schema and `User.activate()` is implemented; the controller and the
  email-sending pipeline are not).
- Password reset (the `password_reset_tokens` table will be added in a later
  migration; the endpoint is documented in the foundation but not implemented).
- Admin endpoints beyond user creation (list / get / status change / role
  assignment will be added in a later phase).
- Kafka is optional; the service runs without a broker.
- Redis / rate-limiting is implemented at the API gateway (Phase 9), not here.
- `UserRegistered` consumer auto-provisioning (will be implemented in
  `patient-service` and `doctor-service`).
