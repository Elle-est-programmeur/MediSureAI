package com.example.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientChatRequest {

    @NotBlank(message = "query is required")
    private String query;

    private String sessionId;
}