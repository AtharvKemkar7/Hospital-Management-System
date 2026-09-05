package com.healthcare.doctor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Doctor professional-profile aggregate. Owned exclusively by the
 * Doctor Service. The link to the Auth Service identity is the opaque
 * {@code userId} (UUID). It is a plain UUID column with no foreign key.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "license_number", nullable = false, unique = true, length = 64)
    private String licenseNumber;

    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @Column(name = "sub_specialty", length = 100)
    private String subSpecialty;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DoctorStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Doctor() {
        // JPA
    }

    public static Doctor create(UUID userId, String firstName, String lastName,
                                String licenseNumber, String specialty,
                                String subSpecialty, String department,
                                String phone, String email) {
        Doctor d = new Doctor();
        d.id = UUID.randomUUID();
        d.userId = Objects.requireNonNull(userId, "userId");
        d.firstName = Objects.requireNonNull(firstName, "firstName").trim();
        d.lastName = Objects.requireNonNull(lastName, "lastName").trim();
        d.licenseNumber = Objects.requireNonNull(licenseNumber, "licenseNumber").trim();
        d.specialty = Objects.requireNonNull(specialty, "specialty").trim();
        d.subSpecialty = subSpecialty == null ? null : subSpecialty.trim();
        d.department = department == null ? null : department.trim();
        d.phone = phone == null ? null : phone.trim();
        d.email = email == null ? null : email.trim().toLowerCase();
        d.status = DoctorStatus.ACTIVE;
        return d;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = DoctorStatus.ACTIVE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void applyUpdate(String firstName, String lastName,
                            String licenseNumber, String specialty,
                            String subSpecialty, String department,
                            String phone, String email) {
        if (firstName != null) this.firstName = firstName.trim();
        if (lastName != null) this.lastName = lastName.trim();
        if (licenseNumber != null) this.licenseNumber = licenseNumber.trim();
        if (specialty != null) this.specialty = specialty.trim();
        if (subSpecialty != null) this.subSpecialty = subSpecialty.trim();
        if (department != null) this.department = department.trim();
        if (phone != null) this.phone = phone.trim();
        if (email != null) this.email = email.trim().toLowerCase();
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getSpecialty() { return specialty; }
    public String getSubSpecialty() { return subSpecialty; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public DoctorStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
