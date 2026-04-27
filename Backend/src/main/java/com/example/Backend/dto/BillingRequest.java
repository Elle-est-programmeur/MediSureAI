package com.example.Backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingRequest {

    private Long recordId;

    @NotNull(message = "totalCost is required")
    private BigDecimal totalCost;

    private String description;
}