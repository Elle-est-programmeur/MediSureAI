package com.example.Backend.dto;

import com.example.Backend.model.ReasoningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflectionResult {

    private ReasoningPath path;
    private boolean isValid;
    private boolean factsCorrect;
    private boolean hasContradictions;
    private List<String> strengths;
    private List<String> weaknesses;
    private String overallAssessment;
}