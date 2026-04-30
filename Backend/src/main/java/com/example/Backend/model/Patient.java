package com.example.Backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private Users user;

    @Column(name = "medical_record_number", unique = true, length = 16)
    private String medicalRecordNumber;

    private String name;

    private Integer age;

    private String gender;

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Billing> billingHistory = new ArrayList<>();

    @PrePersist
    public void assignMrnIfMissing() {
        if (medicalRecordNumber == null || medicalRecordNumber.isBlank()) {
            medicalRecordNumber = "MRN-" + java.util.UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 8).toUpperCase();
        }
    }
}