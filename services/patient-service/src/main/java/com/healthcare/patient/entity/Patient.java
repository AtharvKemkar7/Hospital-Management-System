package com.healthcare.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Patient profile aggregate. Owned exclusively by the Patient Service.
 *
 * <p>The link to the Auth Service identity is the opaque {@code userId}
 * (UUID). It is a plain UUID column with no foreign key — the Auth
 * Service database is not accessible from this service.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    private Gender gender;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PatientStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Patient() {
        // JPA
    }

    public static Patient create(UUID userId, String firstName, String lastName,
                                 LocalDate dateOfBirth, Gender gender,
                                 String phone, String email) {
        Patient p = new Patient();
        p.id = UUID.randomUUID();
        p.userId = Objects.requireNonNull(userId, "userId");
        p.firstName = Objects.requireNonNull(firstName, "firstName").trim();
        p.lastName = Objects.requireNonNull(lastName, "lastName").trim();
        p.dateOfBirth = dateOfBirth;
        p.gender = gender;
        p.phone = phone == null ? null : phone.trim();
        p.email = email == null ? null : email.trim().toLowerCase();
        p.status = PatientStatus.ACTIVE;
        return p;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = PatientStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void applyUpdate(String firstName, String lastName,
                            LocalDate dateOfBirth, Gender gender,
                            String phone, String email) {
        if (firstName != null) this.firstName = firstName.trim();
        if (lastName != null) this.lastName = lastName.trim();
        if (dateOfBirth != null) this.dateOfBirth = dateOfBirth;
        if (gender != null) this.gender = gender;
        if (phone != null) this.phone = phone.trim();
        if (email != null) this.email = email.trim().toLowerCase();
    }

    public void activate() {
        this.status = PatientStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = PatientStatus.INACTIVE;
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public PatientStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
