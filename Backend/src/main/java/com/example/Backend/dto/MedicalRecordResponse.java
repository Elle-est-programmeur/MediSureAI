package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordResponse {
    private Long id;
    private String diagnosis;
    private String treatmentPlan;
    private String doctorName;
    private String doctorSpecialization;
    private List<DrugDTO> drugs;
    private LocalDateTime createdAt;
}