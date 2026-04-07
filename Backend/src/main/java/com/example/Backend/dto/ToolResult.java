package com.example.Backend.dto;

import com.example.Backend.model.ToolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private ToolType toolType;
    private boolean success;
    private String content;
    private Double confidence;
    private String errorMessage;
    private long executionTimeMs;
}