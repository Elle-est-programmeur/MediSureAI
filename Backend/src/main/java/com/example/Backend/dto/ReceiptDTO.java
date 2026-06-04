package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDTO {
    private Long id;
    private Long billingId;
    private String receiptNumber;
    private String transactionRef;
    private String paymentMethod;
    private BigDecimal amount;
    private String patientName;
    private String description;
    private LocalDateTime issuedAt;
}
