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
@Table(name = "receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false, unique = true)
    private Billing billing;

    @Column(nullable = false, length = 32, unique = true)
    private String receiptNumber;

    @Column(nullable = false, length = 64)
    private String transactionRef;

    @Column(nullable = false, length = 32)
    private String paymentMethod;

    @Column(nullable = false)
    private BigDecimal amount;

    private String patientName;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime issuedAt;
}
