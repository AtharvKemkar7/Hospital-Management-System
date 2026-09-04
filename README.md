# Healthcare Hospital Operations Platform

A production-grade, microservices-based platform for hospital operations: patients, doctors, appointments, medical records, prescriptions, billing, and notifications.

> **Status:** Project foundation only. No microservice has been implemented yet.
> Services will be built **one at a time**, in the order defined in [`docs/development-roadmap.md`](docs/development-roadmap.md).

---

## 1. Overview

The platform enables a hospital to:

- Manage patient and doctor identities (beyond raw auth)
- Schedule, confirm, cancel, and complete appointments
- Create and access medical records (least-privilege)
- Issue prescriptions tied to completed appointments
- Generate invoices, record payments
- Send notifications (in-app and email initially)
- Authenticate and authorize via JWT-based SSO across all services

### Technology Stack

| Layer            | Technology                                                |
|------------------|------------------------------------------------------------|
| Backend          | Java 21 LTS, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Build            | Maven                                                      |
| API style        | REST (JSON), versioned `/api/v1/...`                        |
| Database         | PostgreSQL (one per service — *database-per-service*)        |
| Cache            | Redis (auth tokens, rate limiting, hot reads)               |
| Messaging        | Apache Kafka (asynchronous workflows only)                  |
| Object storage   | MinIO (medical record attachments, exports)                 |
| API gateway      | Spring Cloud Gateway                                       |
| Frontend         | Angular (implemented later)                                 |
| Migrations       | Flyway                                                     |
| Observability    | Spring Boot Actuator now; Prometheus / Grafana / OTel later |

---

## 2. Repository Structure

```
healthcare-platform/
├── services/
│   ├── auth-service/
│   ├── patient-service/
│   ├── doctor-service/
│   ├── appointment-service/
│   ├── medical-record-service/
│   ├── prescription-service/
│   ├── billing-service/
│   └── notification-service/
│
├── gateway/
│   └── api-gateway/
│
├── frontend/                    # Angular app (added later)
│
├── infrastructure/
│   └── local/                   # Docker Compose assets (added in phase 12)
│
├── docs/                        # Architecture & design documents
│   ├── architecture.md
│   ├── service-boundaries.md
│   ├── database-design.md
│   ├── api-design.md
│   ├── events.md
│   └── development-roadmap.md
│
├── docker-compose.yml           # Added in phase 12
├── .env.example                 # Added in phase 12
├── .gitignore
└── README.md
```

Each backend service is **independently buildable** with its own `pom.xml` and Spring Boot entry point.

---

## 3. Architectural Principles (summary)

The full set lives in [`docs/architecture.md`](docs/architecture.md). The non-negotiables:

1. **Database per service** — never read or write another service's database.
2. **DTOs at the edge** — JPA entities are never returned from REST controllers.
3. **Thin controllers** — all business logic lives in service classes.
4. **Synchronous REST for reads / immediate responses**, **Kafka for async workflows**.
5. **JWT-based auth** issued by `auth-service` and validated by the gateway and downstream services.
6. **Least-privilege authorization** — patients see only their own medical data; doctors see only records they are authorized to access; billing staff cannot read clinical content unless explicitly granted.
7. **No secrets in source** — configuration via environment variables and Spring profiles.

## 5. Security Posture (initial)

- JWT access tokens (short-lived) + refresh tokens (longer-lived, rotated)
- BCrypt password hashing (cost ≥ 12)
- Role-based access control: `PATIENT`, `DOCTOR`, `ADMIN`, `RECEPTIONIST`, `BILLING_STAFF`
- Resource-ownership checks at the service layer
- Validation on all incoming DTOs (Bean Validation)
- No stack traces in API responses
- Secrets externalized via environment variables
- Per-IP and per-user rate limiting at the gateway
- CORS allow-list configured per environment
- Audit logging for medical-record access (added when `medical-record-service` is built)

---

## 6. Development Phases

Full roadmap: [`docs/development-roadmap.md`](docs/development-roadmap.md).

| #  | Phase                  | Status         |
|----|------------------------|----------------|
| 1  | Auth Service           | Substantially complete (entities, repositories, services, controllers, security, Flyway V1 migration, unit tests) |
| 2  | Patient Service        | Not started    |
| 3  | Doctor Service         | Not started    |
| 4  | Appointment Service    | Not started    |
| 5  | Medical Records        | Not started    |
| 6  | Prescription Service   | Not started    |
| 7  | Billing Service        | Not started    |
| 8  | Notification Service   | Not started    |
| 9  | API Gateway            | Not started    |
| 10 | Integration testing    | Not started    |
| 11 | Frontend (Angular)     | Not started    |
| 12 | Docker                 | Not started    |
| 13 | CI/CD                  | Not started    |
| 14 | Kubernetes             | Not started    |
| 15 | Terraform              | Not started    |
| 16 | Cloud deployment       | Not started    |
| 17 | Observability          | Not started    |
| 18 | Security hardening     | Not started    |
| 19 | Reliability engineering| Not started    |

> **We are currently at phase 0 (foundation).**

---

## 7. Local Development (later)

The project does not yet provide a working local stack. Once phase 12 starts, this README will document how to run:

```bash
cp .env.example .env
docker compose up -d
./mvnw -pl services/auth-service spring-boot:run
```

For now, only the documentation in `/docs` is authoritative.

---

## 8. Documentation Index

- [Architecture](docs/architecture.md) — context, containers, cross-cutting concerns
- [Service boundaries](docs/service-boundaries.md) — per-service responsibilities and contracts
- [Database design](docs/database-design.md) — per-service schemas, naming, migrations
- [API design](docs/api-design.md) — REST conventions, error format, versioning
- [Events](docs/events.md) — Kafka topics, envelopes, producers, consumers
- [Development roadmap](docs/development-roadmap.md) — phased build order and exit criteria

---

## 9. License

Internal project. License to be defined before any external contribution.

8. **No sensitive data in logs** — passwords, JWTs, refresh tokens, and unnecessary clinical data must never be logged.
9. **Versioned APIs** — all endpoints under `/api/v1/...`.
10. **Eventual consistency between services** — services own their data; cross-service state is reconciled via events.

---

## 4. Microservices (high level)

| #  | Service                  | Owns                                                | DB                  |
|----|--------------------------|-----------------------------------------------------|---------------------|
| 1  | `auth-service`           | Users, roles, refresh tokens                        | `auth_db`           |
| 2  | `patient-service`        | Patient profiles, insurance, emergency contacts     | `patient_db`        |
| 3  | `doctor-service`         | Doctor profiles, specialties, availability          | `doctor_db`         |
| 4  | `appointment-service`    | Appointments, statuses, slots                       | `appointment_db`    |
| 5  | `medical-record-service` | Medical records, diagnoses, allergies, attachments  | `medical_record_db` |
| 6  | `prescription-service`   | Prescriptions, prescription items                   | `prescription_db`   |
| 7  | `billing-service`        | Invoices, invoice items, payments                   | `billing_db`        |
| 8  | `notification-service`   | Notifications, preferences, templates               | `notification_db`   |
| 9  | `api-gateway`            | Routing, JWT validation, rate limiting, CORS        | stateless           |

Full responsibilities, contracts, and entity ownership are documented in [`docs/service-boundaries.md`](docs/service-boundaries.md).
