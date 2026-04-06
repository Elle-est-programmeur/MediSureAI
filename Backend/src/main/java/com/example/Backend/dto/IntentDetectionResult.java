package com.example.Backend.dto;

import com.example.Backend.model.QueryIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentDetectionResult {
    private QueryIntent intent;
    private Double confidence;
    private String reasoning;
}