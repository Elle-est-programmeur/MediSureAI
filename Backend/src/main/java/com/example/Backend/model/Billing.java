package com.example.Backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private MedicalRecord record;

    @Column(nullable = false)
    private BigDecimal totalCost;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) default 'PENDING'")
    @Builder.Default
    private BillingStatus status = BillingStatus.PENDING;

    @Column(length = 32)
    private String paymentMethod;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
