package com.example.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MedicalRecordRequest {

    @NotNull(message = "patientUserId is required")
    private Long patientUserId;

    @NotBlank(message = "diagnosis is required")
    private String diagnosis;

    private String treatmentPlan;

    private List<DrugDTO> drugs;
}