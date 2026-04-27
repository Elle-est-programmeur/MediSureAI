package com.example.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorProfileRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String specialization;

    private String hospitalId;
}