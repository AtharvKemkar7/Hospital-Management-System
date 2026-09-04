# Events (Kafka)

This document defines the asynchronous event contract for the platform: topic naming, envelope schema, per-event payloads, producer/consumer responsibilities, ordering, retention, and security.

> REST contracts are in [`api-design.md`](api-design.md) and per-service endpoint lists in [`service-boundaries.md`](service-boundaries.md).

---

## 1. Why Kafka, and only where it matters

We use **Apache Kafka** for asynchronous, eventually-consistent workflows:

- Things that **fan out** to many consumers (a new appointment notifies the patient, the doctor, and starts an audit).
- Things that **don't block the user** (issuing an invoice after an appointment is completed).
- Things that **must be retried** independently of the request that triggered them (delivering a notification, persisting an audit row).

We deliberately **do not** use Kafka for:

- Reads that need an immediate answer ("is patient X active?") — these go through REST with a Redis cache.
- Single-consumer, real-time RPCs — REST is simpler, faster, and easier to authorize.

---

## 2. Topic naming

Topics live under a single platform namespace and are namespaced by domain:

```
healthcare.<domain>.<event-past-tense>
```

Examples:

- `healthcare.auth.UserRegistered`
- `healthcare.appointment.AppointmentCreated`
- `healthcare.billing.InvoiceCreated`
- `healthcare.notification.NotificationDelivered`

Rules:

- Domain is the **owning service's** domain (e.g., events from `appointment-service` live under `healthcare.appointment.*`).
- Event names are in **PascalCase, past tense** (`AppointmentCancelled`, not `CancelAppointment`).
- Dead-letter topics for any consumer are `healthcare.<domain>.<event>.dlq`.

---

## 3. Cluster, partitions, and retention

- **Local dev:** 1 broker, replication factor 1, single partition per topic.
- **Production:** 3 brokers, replication factor 3, partitions sized for the expected throughput of the topic.
- Partition keys are the **aggregate ID** (e.g., `appointmentId` for appointment events). This guarantees per-aggregate ordering.
- Retention:
  - Domain topics: **30 days** (configurable per topic).
  - Dead-letter topics: **90 days** for forensic review.
- Topic creation is managed by the platform team via Terraform / Kafka Admin API; services do not auto-create topics with `auto.create.topics.enable`.

---

## 4. Envelope

Every event payload, regardless of topic, is wrapped in a standard envelope:

```json
{
  "eventId":     "b3d4e6c1-2f4a-4c5b-9e0f-1a2b3c4d5e6f",
  "eventType":   "AppointmentCreated",
  "eventVersion": "1",
  "occurredAt":  "2026-04-09T10:15:00.123Z",
  "producer":    "appointment-service",
  "correlationId": "a1b2c3d4-e5f6-7890-1234-56789abcdef0",
  "payload": { ... }
}
```

| Field          | Purpose                                                                  |
|----------------|--------------------------------------------------------------------------|
| `eventId`      | Unique per event. Used by consumers for idempotent dedup.                 |
| `eventType`    | Mirrors the topic's event name; allows a single topic for multiple types in the future. |

## 6. Topic catalog

The full catalog of events. Each row points to the service that **produces** the event and the services that **consume** it. Schema details follow below.

| Topic                                      | Producer                | Consumers                                          |
|--------------------------------------------|-------------------------|----------------------------------------------------|
| `healthcare.auth.UserRegistered`           | `auth-service`          | `patient-service`, `doctor-service`, `notification-service` |
| `healthcare.auth.UserDeactivated`          | `auth-service`          | `patient-service`, `doctor-service`                |
| `healthcare.patient.PatientCreated`        | `patient-service`       | `notification-service`                             |
| `healthcare.patient.PatientUpdated`        | `patient-service`       | (none in foundation)                               |
| `healthcare.patient.PatientDeactivated`    | `patient-service`       | `appointment-service`, `billing-service`           |
| `healthcare.doctor.DoctorCreated`          | `doctor-service`        | `notification-service`                             |
| `healthcare.doctor.DoctorUpdated`          | `doctor-service`        | (none in foundation)                               |
| `healthcare.doctor.DoctorDeactivated`      | `doctor-service`        | `appointment-service`                              |
| `healthcare.appointment.AppointmentCreated`| `appointment-service`   | `notification-service`                             |
| `healthcare.appointment.AppointmentConfirmed` | `appointment-service` | `notification-service`                             |
| `healthcare.appointment.AppointmentCancelled`  | `appointment-service` | `notification-service`                             |
| `healthcare.appointment.AppointmentRescheduled` | `appointment-service` | `notification-service`                            |
| `healthcare.appointment.AppointmentCompleted`  | `appointment-service` | `medical-record-service`, `prescription-service`, `billing-service`, `notification-service` |
| `healthcare.medical-record.MedicalRecordCreated` | `medical-record-service` | `notification-service`                         |
| `healthcare.medical-record.MedicalRecordUpdated` | `medical-record-service` | `notification-service`                         |
| `healthcare.prescription.PrescriptionCreated`   | `prescription-service` | `billing-service`, `notification-service`          |
| `healthcare.prescription.PrescriptionUpdated`   | `prescription-service` | `notification-service`                            |
| `healthcare.billing.InvoiceCreated`        | `billing-service`       | `notification-service`                             |
| `healthcare.billing.PaymentCompleted`      | `billing-service`       | `notification-service`                             |
| `healthcare.notification.NotificationDelivered` | `notification-service` | (none)                                          |
| `healthcare.notification.NotificationFailed`   | `notification-service` | (none)                                          |

### Aggregate ID → partition key

| Topic family            | Partition key            |
|-------------------------|--------------------------|
| auth.*                  | `userId`                  |
| patient.*               | `patientId`               |
| doctor.*                | `doctorId`                |
| appointment.*           | `appointmentId`           |

## 7. Event payloads (v1)

> Field types are JSON-schema-style. `"uuid"` is a string in UUID format, `"datetime"` is ISO-8601 UTC, `"money"` is a number with 2 decimal places in the smallest currency unit unless otherwise noted.

### 7.1 `healthcare.auth.UserRegistered` (v1)

> **Privacy note (v1):** this event intentionally does **not** include the user's email or any other personal data. Personal information is fetched by downstream services over the authenticated REST API (`GET /api/v1/users/{id}`) when it is actually needed. This minimizes the blast radius of any future event-bus exposure and aligns with the platform's "PHI never goes in events" rule.

```json
{
  "userId":        "uuid",
  "role":          "PATIENT | DOCTOR | ADMIN | RECEPTIONIST | BILLING_STAFF",
  "emailHint":     "string (non-reversible masked form, e.g. \"j***e@example.com\")",
  "occurredAt":    "datetime"
}
```

- Partition key: `userId`.
- Consumers:
  - `patient-service` if `role == "PATIENT"` → creates a patient profile.
  - `doctor-service` if `role == "DOCTOR"` → creates a doctor profile.
  - `notification-service` → sends a "welcome" notification.

### 7.2 `healthcare.auth.UserDeactivated` (v1)

```json
{ "userId": "uuid", "reason": "string", "occurredAt": "datetime" }
```

### 7.3 `healthcare.patient.PatientCreated` (v1)

```json
{ "patientId": "uuid", "userId": "uuid", "occurredAt": "datetime" }
```

### 7.4 `healthcare.patient.PatientDeactivated` (v1)

```json
{ "patientId": "uuid", "reason": "string", "occurredAt": "datetime" }
```

### 7.5 `healthcare.doctor.DoctorCreated` (v1)

```json
{ "doctorId": "uuid", "userId": "uuid", "specialty": "string", "occurredAt": "datetime" }
```

### 7.6 `healthcare.doctor.DoctorDeactivated` (v1)

```json
{ "doctorId": "uuid", "reason": "string", "occurredAt": "datetime" }
```

### 7.7 `healthcare.appointment.AppointmentCreated` (v1)

```json
{
  "appointmentId": "uuid",
  "patientId":     "uuid",
  "doctorId":      "uuid",
  "startAt":       "datetime",
  "endAt":         "datetime",
  "type":          "IN_PERSON | TELEHEALTH",
  "occurredAt":    "datetime"
}
```

### 7.8 `healthcare.appointment.AppointmentConfirmed` (v1)

Same payload as `AppointmentCreated` (the event is a state transition, the data is identical).

### 7.9 `healthcare.appointment.AppointmentCancelled` (v1)

```json

### 7.11 `healthcare.appointment.AppointmentCompleted` (v1)

```json
{
  "appointmentId": "uuid",
  "patientId":     "uuid",
  "doctorId":      "uuid",
  "completedAt":   "datetime",
  "occurredAt":    "datetime"
}
```

> **Note:** No clinical data is included in any appointment event. Clinical content is added by `medical-record-service` after the appointment and is published separately.

### 7.12 `healthcare.medical-record.MedicalRecordCreated` (v1)

```json
{
  "recordId":      "uuid",
  "patientId":     "uuid",
  "doctorId":      "uuid",
  "appointmentId": "uuid",
  "occurredAt":    "datetime"
}
```

> Only IDs are published. Clinical content (diagnoses, vitals, attachments) is **never** published in events; consumers needing it must call the REST API with proper authorization. This is a hard privacy boundary.

### 7.13 `healthcare.medical-record.MedicalRecordUpdated` (v1)

```json
{ "recordId": "uuid", "patientId": "uuid", "occurredAt": "datetime" }
```

### 7.14 `healthcare.prescription.PrescriptionCreated` (v1)

```json
{
  "prescriptionId": "uuid",
  "patientId":      "uuid",
  "doctorId":       "uuid",
  "appointmentId":  "uuid",
  "itemCount":      "int",
  "occurredAt":     "datetime"
}
```

> Drug names and dosages are **not** in the event. They are PHI; only the aggregate is broadcast.

### 7.15 `healthcare.prescription.PrescriptionUpdated` (v1)

```json
{ "prescriptionId": "uuid", "patientId": "uuid", "status": "ISSUED | CANCELLED | DISPENSED", "occurredAt": "datetime" }
```

### 7.16 `healthcare.billing.InvoiceCreated` (v1)

```json
{
  "invoiceId":      "uuid",
  "patientId":      "uuid",
  "appointmentId":  "uuid",
  "totalAmount":    "number",
  "currency":       "string (ISO 4217)",
  "occurredAt":     "datetime"
}
```

### 7.17 `healthcare.billing.PaymentCompleted` (v1)

```json
{
  "paymentId":     "uuid",

## 8. Producer responsibilities

- Assign a **unique** `eventId` per event (UUIDv4) and embed it in both the envelope and the Kafka header.
- Set the **partition key** to the aggregate ID so events for the same aggregate land in the same partition (preserves ordering).
- Populate every mandatory Kafka header listed in §5.
- Set `correlation-id` from the inbound HTTP request (or generate a new one if the event originates from a scheduled job).
- Send with `acks=all` and a small retry policy; surface failures to the calling transaction.
- Log the `eventId` and topic at `INFO`; never log the payload if it contains PHI.

### Outbox pattern (deferred)

For services that must publish an event as part of a database transaction, the **outbox pattern** is the recommended approach:

- Insert into an `outbox` table inside the same transaction.
- A separate poller publishes from the outbox to Kafka and marks rows as published.
- The poller is idempotent (uses `eventId`).

This is implemented in `appointment-service` and `billing-service` when they are built; not needed in the foundation.

---

## 9. Consumer responsibilities

- **Idempotency:** every consumer maintains a `processed_events` table keyed on `eventId`. Re-deliveries are dropped.
- **Ordering:** consumers process events in partition order. A single partition is owned by a single consumer instance.
- **Retries:** transient failures (DB blip, downstream HTTP 503) trigger a back-off retry up to 5 times within the same poll loop.
- **Dead letter:** after 5 failed attempts, the event is forwarded to `<topic>.dlq` with the failure reason in a header and the original payload preserved.
- **Schema tolerance:** unknown fields are ignored. Missing required fields → reject and DLQ.
- **Anti-corruption:** the consumer's first line of code on receiving an event is a mapper from the foreign DTO to the consumer's internal model.

### Consumer groups

| Service                  | Consumer group                                  |
|--------------------------|-------------------------------------------------|
| `patient-service`        | `patient-service`                                |
| `doctor-service`         | `doctor-service`                                 |
| `appointment-service`    | `appointment-service`                            |
| `medical-record-service` | `medical-record-service`                         |
| `prescription-service`   | `prescription-service`                           |
| `billing-service`        | `billing-service`                                |
| `notification-service`   | `notification-service` (single group for now)    |

A new service is started with its own consumer group so that it consumes the full topic from the beginning (or from a configurable offset). When a service re-deploys, the consumer group is preserved (offsets move forward).

---

## 10. Security and privacy in events

- **PHI never goes in events.** Only IDs and minimal context. Drug names, dosages, diagnoses, and notes are out of scope for Kafka.
- Events are not encrypted at the application layer; we rely on **TLS in transit** (Kafka brokers and clients) and **at-rest encryption** in the storage layer (LUKS / cloud KMS) added in phase 16.
- Producer and consumer credentials are read from environment variables, never from source.
- Every consumer MUST validate the `producer` header against an allow-list (added when this becomes relevant in a multi-team environment; not enforced in the foundation single-team setup).
- Audit trail: a sample of events (1% by default) is mirrored to a separate `healthcare.audit.event-mirror` topic for forensic analysis. This is added in phase 18 (security hardening).

---

## 11. Local development (later)

- Local Kafka runs in Docker (phase 12) with a single broker, replication factor 1, and `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`.
- A `kafka-init` container creates the topics listed in §6 on first start, with one partition each.
- A `kafka-ui` container is available on `http://localhost:8081` to browse topics and payloads (only on local profile).

---

## 12. Cross-references

- REST contracts: [`api-design.md`](api-design.md)
- Per-service responsibilities: [`service-boundaries.md`](service-boundaries.md)
- Roadmap: [`development-roadmap.md`](development-roadmap.md)

  "invoiceId":     "uuid",
  "patientId":     "uuid",
  "amount":        "number",
  "currency":      "string (ISO 4217)",
  "method":        "CASH | CARD | INSURANCE | ONLINE",
  "occurredAt":    "datetime"
}
```

### 7.18 `healthcare.notification.NotificationDelivered` (v1)

```json
{ "notificationId": "uuid", "recipientUserId": "uuid", "channel": "IN_APP | EMAIL", "occurredAt": "datetime" }
```

### 7.19 `healthcare.notification.NotificationFailed` (v1)

```json
{ "notificationId": "uuid", "recipientUserId": "uuid", "reason": "string", "occurredAt": "datetime" }
```

---

{
  "appointmentId": "uuid",
  "patientId":     "uuid",
  "doctorId":      "uuid",
  "reason":        "string",
  "cancelledBy":   "PATIENT | DOCTOR | RECEPTIONIST | SYSTEM",
  "occurredAt":    "datetime"
}
```

### 7.10 `healthcare.appointment.AppointmentRescheduled` (v1)

```json
{
  "appointmentId": "uuid",
  "patientId":     "uuid",
  "doctorId":      "uuid",
  "oldStartAt":    "datetime",
  "newStartAt":    "datetime",
  "occurredAt":    "datetime"
}
```

---

| medical-record.*        | `recordId`                |
| prescription.*          | `prescriptionId`          |
| billing.*               | `invoiceId`               |
| notification.*          | `notificationId`          |

---

| `eventVersion` | Starts at `"1"`. Bumped only on a **breaking** schema change.            |
| `occurredAt`   | Producer's clock when the event was emitted (UTC).                        |
| `producer`     | `service.name`. Useful for routing and observability.                    |
| `correlationId`| Same value as the originating HTTP request, if any.                       |
| `payload`      | The event-specific data, defined per event type.                          |

### Schema evolution

- New fields MAY be added at any time. Consumers MUST ignore unknown fields.
- Renaming or removing a field requires bumping `eventVersion` and publishing under a new topic (`healthcare.appointment.AppointmentCreated.v2`) until consumers migrate.
- The platform team owns the schema registry (added in phase 17 — observability/confluent-schema-registry is **not** required in the foundation).

### Serialization

- All payloads are **JSON** (`application/json`).
- Keys are strings (the aggregate ID).

---

## 5. Kafka headers (mandatory)

Every message carries the following Kafka headers so consumers and operators can trace and audit without parsing the body:

| Header          | Value                                                        |
|-----------------|--------------------------------------------------------------|
| `content-type`  | `application/json`                                            |
| `event-id`      | Same as `eventId` in the body                                 |
| `event-type`    | Same as `eventType` in the body                               |
| `event-version` | Same as `eventVersion` in the body                            |
| `producer`      | The producing service's `service.name`                        |
| `correlation-id`| The originating request's correlation ID (if any)             |
| `occurred-at`   | ISO-8601 timestamp                                            |

---
