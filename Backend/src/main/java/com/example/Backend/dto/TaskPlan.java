package com.example.Backend.dto;

import com.example.Backend.model.AgentStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlan {
    private List<AgentStep> steps;
    private Map<String, Object> parameters;
    private String reasoning;
}