package com.example.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayBillRequest {

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;
}
