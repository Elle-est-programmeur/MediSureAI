package com.example.Backend.dto;

import com.example.Backend.model.BillingStatus;
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
public class BillingResponse {
    private Long id;
    private BigDecimal totalCost;
    private String description;
    private LocalDateTime createdAt;
    private Long recordId;
    private String recordDiagnosis;
    private String patientName;
    private BillingStatus status;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private ReceiptDTO receipt;
}
