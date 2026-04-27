package com.example.Backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientProfileRequest {

    @NotBlank(message = "name is required")
    private String name;

    @Min(value = 0, message = "age must be non-negative")
    private Integer age;

    private String gender;
}